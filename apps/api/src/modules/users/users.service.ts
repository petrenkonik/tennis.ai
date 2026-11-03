import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Analysis, AnalysisDocument } from '../../common/schemas/analysis.schema';
import { Subscription, SubscriptionDocument } from '../../common/schemas/subscription.schema';
import { User, UserDocument } from '../../common/schemas/user.schema';
import type { QuotaState, UserDto } from '@tennis-ai/shared';

// Порт квот оригинала: free = 9 видеоанализов навсегда; pro = 20/мес + unlimited real-time.
const FREE_LIFETIME_ANALYSES = 9;
const PRO_MONTHLY_ANALYSES = 20;

@Injectable()
export class UsersService {
  constructor(
    @InjectModel(User.name) private readonly users: Model<UserDocument>,
    @InjectModel(Subscription.name) private readonly subs: Model<SubscriptionDocument>,
    @InjectModel(Analysis.name) private readonly analyses: Model<AnalysisDocument>,
  ) {}

  async me(userId: string): Promise<UserDocument> {
    const user = await this.users.findById(userId);
    if (!user) throw new NotFoundException('user not found');
    return user;
  }

  async updateProfile(
    userId: string,
    patch: { level?: string; dominantHand?: 'left' | 'right'; heightCm?: number; language?: string },
  ): Promise<UserDocument> {
    const set: Record<string, unknown> = {};
    if (patch.level !== undefined) set['profile.level'] = patch.level;
    if (patch.dominantHand !== undefined) set['profile.dominantHand'] = patch.dominantHand;
    if (patch.heightCm !== undefined) set['profile.heightCm'] = patch.heightCm;
    if (patch.language !== undefined) set['language'] = patch.language;
    const user = await this.users.findByIdAndUpdate(userId, { $set: set }, { new: true });
    if (!user) throw new NotFoundException('user not found');
    return user;
  }

  async quotaState(userId: string): Promise<QuotaState> {
    const now = new Date();
    const active = await this.subs
      .findOne({ userId, status: 'active', periodEnd: { $gt: now } })
      .sort({ periodEnd: -1 });

    if (active) {
      const monthStart = new Date(now.getFullYear(), now.getMonth(), 1);
      const used = await this.analyses.countDocuments({ userId, createdAt: { $gte: monthStart } });
      return { plan: 'pro', analysesUsed: used, analysesLimit: PRO_MONTHLY_ANALYSES, periodEndsAt: active.periodEnd.toISOString() };
    }

    const used = await this.analyses.countDocuments({ userId });
    return { plan: 'free', analysesUsed: used, analysesLimit: FREE_LIFETIME_ANALYSES };
  }

  async canCreateAnalysis(userId: string): Promise<{ allowed: boolean; quota: QuotaState }> {
    const quota = await this.quotaState(userId);
    return { allowed: quota.analysesLimit === null || quota.analysesUsed < quota.analysesLimit, quota };
  }

  toDto(user: UserDocument, quota: QuotaState): UserDto {
    return {
      id: String(user._id),
      telegramId: user.telegramId,
      firstName: user.firstName,
      username: user.username,
      photoUrl: user.photoUrl,
      profile: user.profile ?? {},
      language: user.language,
      xp: user.xp,
      quota,
    };
  }
}
