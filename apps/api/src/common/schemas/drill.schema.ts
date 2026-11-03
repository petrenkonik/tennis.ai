import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument } from 'mongoose';

/** Drill library (port of drill_service: objective / setup / progression / reps / sets / difficulty / targetMetrics). */
@Schema({ collection: 'drills', timestamps: true })
export class Drill {
  @Prop({ required: true, unique: true })
  slug!: string;

  @Prop({ required: true })
  title!: string;

  @Prop()
  objective?: string;

  @Prop()
  setup?: string;

  @Prop({ type: [String], default: [] })
  howTo!: string[];

  @Prop({ type: [String], default: [] })
  keyPoints!: string[];

  @Prop()
  progression?: string;

  @Prop({ default: '3x10' })
  repsSets!: string;

  @Prop({ enum: ['beginner', 'intermediate', 'advanced', 'pro'], default: 'beginner' })
  difficulty!: string;

  /** Metric codes (packages/shared metrics.json) that the drill trains. */
  @Prop({ type: [String], default: [], index: true })
  targetMetrics!: string[];
}

export type DrillDocument = HydratedDocument<Drill>;
export const DrillSchema = SchemaFactory.createForClass(Drill);
