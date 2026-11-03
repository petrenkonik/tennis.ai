import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument } from 'mongoose';

/** Analysis session (port of the original analyses: strokeType, moments M0–M6, green/red metrics, overall_score). */
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

  /** Trim window from VIDEO PREVIEW (port of the original _previewTrim); null = clip not trimmed. */
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
