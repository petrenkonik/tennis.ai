import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Drill, DrillDocument } from '../../common/schemas/drill.schema';
import { Analysis, AnalysisDocument } from '../../common/schemas/analysis.schema';

/** Подбор drills по weaknesses анализа (порт drill_recommendation_service). */
@Injectable()
export class DrillsService {
  constructor(
    @InjectModel(Drill.name) private readonly drills: Model<DrillDocument>,
    @InjectModel(Analysis.name) private readonly analyses: Model<AnalysisDocument>,
  ) {}

  async list(filter: { difficulty?: string; targetMetric?: string }): Promise<DrillDocument[]> {
    const q: Record<string, unknown> = {};
    if (filter.difficulty) q.difficulty = filter.difficulty;
    if (filter.targetMetric) q.targetMetrics = filter.targetMetric;
    return this.drills.find(q).sort({ difficulty: 1, title: 1 });
  }

  async bySlug(slug: string): Promise<DrillDocument> {
    const drill = await this.drills.findOne({ slug });
    if (!drill) throw new NotFoundException('drill not found');
    return drill;
  }

  /** "These drills target weaknesses detected in your last analysis" — intersect topWeaknesses × targetMetrics. */
  async recommended(userId: string, analysisId?: string): Promise<DrillDocument[]> {
    let weaknesses: string[] = [];
    if (analysisId) {
      const a = await this.analyses.findById(analysisId);
      if (!a || a.userId !== userId) throw new NotFoundException('analysis not found');
      weaknesses = a.topWeaknesses ?? [];
    } else {
      const last = await this.analyses.findOne({ userId, status: 'completed' }).sort({ createdAt: -1 });
      weaknesses = last?.topWeaknesses ?? [];
    }
    if (weaknesses.length === 0) return this.drills.find().limit(6);
    const matched = await this.drills.find({ targetMetrics: { $in: weaknesses } });
    return matched.length > 0 ? matched : this.drills.find().limit(6);
  }
}
