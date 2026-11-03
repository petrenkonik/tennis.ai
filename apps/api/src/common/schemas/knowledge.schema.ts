import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument } from 'mongoose';

/** База знаний Axel (порт knowledge: домены biomechanics, tactics, fitness, mental, drills, coaching). */
@Schema({ collection: 'knowledge', timestamps: true })
export class KnowledgeDoc {
  @Prop({ required: true })
  title!: string;

  @Prop({ required: true, enum: ['biomechanics', 'tactics', 'fitness', 'mental', 'drills', 'coaching'] })
  domain!: string;

  @Prop({ required: true })
  content!: string;

  @Prop({ type: [String], default: [] })
  tags!: string[];
}

export type KnowledgeDocument = HydratedDocument<KnowledgeDoc>;
export const KnowledgeSchema = SchemaFactory.createForClass(KnowledgeDoc);
