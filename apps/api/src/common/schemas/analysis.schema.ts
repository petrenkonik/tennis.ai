import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument } from 'mongoose';

/** Сессия анализа (порт analyses из оригинала: strokeType, моменты M0–M6, метрики green/red, overall_score). */
@Schema({ collection: 'analyses', timestamps: true })
export class Analysis {
  @Prop({ required: true, index: true })
  userId!: string;

  @Prop({ required: true, enum: ['serve', 'forehand', 'backhand'] })
  strokeType!: string;

  @Prop({ enum: ['created', 'analyzing', 'completed', 'failed'], default: 'created' })
  status!: string;

  @Prop({ type: Object, default: {} })
  videos!: { original?: string; compressed?: string; annotated?: string };

  /** Окно трима из VIDEO PREVIEW (порт _previewTrim оригинала); null = клип не тримилен. */
  @Prop({ type: Number, default: null })
  trimStartSec!: number | null;

  @Prop({ type: Number, default: null })
  trimEndSec!: number | null;

  @Prop({ type: Number, default: 30 })
  fps!: number;

  @Prop({ type: Number, default: 0 })
  frameCount!: number;

  @Prop({ type: Array, default: [] })
  phases!: unknown[];

  @Prop({ type: Array, default: [] })
  metrics!: unknown[];

  @Prop({ type: Object })
  overall?: { score: number | null; greens: number; reds: number; unknown: number };

  @Prop({ type: [String], default: [] })
  topWeaknesses!: string[];

  @Prop({ type: [String], default: [] })
  notes!: string[];
}

export type AnalysisDocument = HydratedDocument<Analysis>;
export const AnalysisSchema = SchemaFactory.createForClass(Analysis);
