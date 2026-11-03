import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument } from 'mongoose';

/** Метаданные файлов (DECISIONS §7.2: бинарники — НЕ в Mongo, только ключи в S3-совместимом хранилище). */
@Schema({ collection: 'videos', timestamps: true })
export class Video {
  @Prop({ required: true, index: true })
  userId!: string;

  @Prop({ required: true, index: true })
  analysisId!: string;

  @Prop({ required: true, enum: ['original', 'compressed', 'annotated', 'frame'] })
  kind!: string;

  @Prop({ required: true })
  key!: string;

  @Prop()
  contentType?: string;

  @Prop({ type: Number, default: 0 })
  sizeBytes?: number;
}

export type VideoDocument = HydratedDocument<Video>;
export const VideoSchema = SchemaFactory.createForClass(Video);
