import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument } from 'mongoose';

@Schema({ collection: 'users', timestamps: true })
export class User {
  @Prop({ required: true, unique: true, index: true })
  telegramId!: number;

  @Prop()
  firstName?: string;

  @Prop()
  username?: string;

  @Prop()
  photoUrl?: string;

  @Prop({ type: Object, default: {} })
  profile!: { level?: string; dominantHand?: 'left' | 'right'; heightCm?: number };

  @Prop({ default: 'en' })
  language!: string;

  @Prop({ default: 0 })
  xp!: number;

  @Prop({ type: [String], default: [] })
  badges!: string[];

  @Prop({ default: 0 })
  currentStreak!: number;

  /** Счётчики квот (порт quota_service: 9 видеоанализов lifetime / 20 real-time сессий для free). */
  @Prop({ type: Object, default: {} })
  quotaCounters!: { lifetimeAnalyses?: number; realTimeSessions?: number };
}

export type UserDocument = HydratedDocument<User>;
export const UserSchema = SchemaFactory.createForClass(User);
