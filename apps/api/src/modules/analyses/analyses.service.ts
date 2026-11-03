import { ForbiddenException, HttpException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import type { AnalysisDto } from '@tennis-ai/shared';
import { Analysis, AnalysisDocument } from '../../common/schemas/analysis.schema';
import { Video, VideoDocument } from '../../common/schemas/video.schema';
import { S3Service } from '../uploads/s3.service';
import { UsersService } from '../users/users.service';
import type { JwtPayload } from '../../common/current-user.decorator';
import { EventsGateway } from '../../ws/events.gateway';
import type { AnalysisResultsDto } from './analyses.dto';

@Injectable()
export class AnalysesService {
  constructor(
    @InjectModel(Analysis.name) private readonly analyses: Model<AnalysisDocument>,
    @InjectModel(Video.name) private readonly videos: Model<VideoDocument>,
    private readonly users: UsersService,
    private readonly s3: S3Service,
    private readonly ws: EventsGateway,
  ) {}

  private async owned(userId: string, id: string): Promise<AnalysisDocument> {
    const doc = await this.analyses.findById(id);
    if (!doc) throw new NotFoundException('analysis not found');
    if (doc.userId !== userId) throw new ForbiddenException();
    return doc;
  }

  async create(u: JwtPayload, dto: { strokeType: string; videoKey: string; trimStartSec?: number; trimEndSec?: number }) {
    const { allowed, quota } = await this.users.canCreateAnalysis(u.sub);
    if (!allowed) {
      // 402: quota exhausted — the client leads the user to the paywall (Stars).
      throw new HttpException(
        `Quota exhausted: ${quota.plan} plan, ${quota.analysesUsed}/${quota.analysesLimit ?? '∞'} analyses used`,
        402,
      );
    }
    const doc = await this.analyses.create({
      userId: u.sub,
      strokeType: dto.strokeType,
      status: 'created',
      videos: { original: dto.videoKey },
      trimStartSec: dto.trimStartSec ?? null,
      trimEndSec: dto.trimEndSec ?? null,
    });
    await this.videos.create({
      userId: u.sub,
      analysisId: String(doc._id),
      kind: 'original',
      key: dto.videoKey,
    });
    return {
      id: String(doc._id),
      status: doc.status,
      quotaRemaining: quota.analysesLimit === null ? null : quota.analysesLimit - quota.analysesUsed - 1,
    };
  }

  async saveResults(u: JwtPayload, id: string, dto: AnalysisResultsDto) {
    await this.owned(u.sub, id);
    const doc = await this.analyses.findByIdAndUpdate(
      id,
      {
        $set: {
          status: 'completed',
          fps: dto.fps,
          frameCount: dto.frameCount,
          phases: dto.phases,
          metrics: dto.metrics,
          overall: dto.overall,
          topWeaknesses: dto.topWeaknesses,
          notes: dto.notes ?? [],
        },
      },
      { new: true },
    );
    this.ws.notifyUser(u.sub, 'analysis:completed', { id });
    return doc!.toJSON();
  }

  async attachAnnotated(u: JwtPayload, id: string, annotatedKey: string) {
    await this.owned(u.sub, id);
    await this.analyses.updateOne({ _id: id }, { $set: { 'videos.annotated': annotatedKey } });
    await this.videos.findOneAndUpdate(
      { analysisId: id, kind: 'annotated' },
      { userId: u.sub, analysisId: id, kind: 'annotated', key: annotatedKey },
      { upsert: true },
    );
    return { ok: true };
  }

  async list(u: JwtPayload, limit = 50): Promise<AnalysisDto[]> {
    const docs = await this.analyses
      .find({ userId: u.sub })
      .sort({ createdAt: -1 })
      .limit(Math.min(100, limit));
    return docs.map((d) => this.toDto(d));
  }

  async get(u: JwtPayload, id: string) {
    return (await this.owned(u.sub, id)).toJSON();
  }

  /**
   * GET /analyses/:id/video — URL for the Results player (port of annotated_video_url:
   * in the original the player plays the annotated video; until it exists — the original one).
   */
  async videoUrl(u: JwtPayload, id: string): Promise<{ url: string | null; expiresInSec: number }> {
    const doc = await this.owned(u.sub, id);
    const key = doc.videos?.annotated ?? doc.videos?.original;
    if (!key) return { url: null, expiresInSec: 0 };
    return { url: await this.s3.presignGet(key), expiresInSec: 3600 };
  }

  /** Cascade delete: Mongo (analyses + videos) + all storage objects of the session (DECISIONS §7.2). */
  async remove(u: JwtPayload, id: string): Promise<void> {
    const doc = await this.owned(u.sub, id);
    await this.videos.deleteMany({ analysisId: id });
    await this.analyses.deleteOne({ _id: id });
    const prefix = `users/${u.sub}/analyses/${id}/`;
    // The sessionId in the key may not match the _id (the client generated it itself) — clean both prefixes.
    await this.s3.deletePrefix(prefix);
    const original = doc.videos?.original;
    if (original && !original.startsWith(prefix)) {
      const sessionDir = original.substring(0, original.lastIndexOf('/') + 1);
      if (sessionDir) await this.s3.deletePrefix(sessionDir);
    }
  }

  private toDto(d: AnalysisDocument): AnalysisDto {
    const createdAt = (d as unknown as { createdAt?: Date }).createdAt;
    return {
      id: String(d._id),
      strokeType: d.strokeType as AnalysisDto['strokeType'],
      status: d.status as AnalysisDto['status'],
      overallScore: d.overall?.score ?? null,
      videos: d.videos ?? {},
      createdAt: (createdAt ?? new Date()).toISOString(),
    };
  }
}
