import { Injectable } from '@nestjs/common';
import {
  DeleteObjectsCommand,
  GetObjectCommand,
  ListObjectsV2Command,
  PutObjectCommand,
  S3Client,
} from '@aws-sdk/client-s3';
import { getSignedUrl } from '@aws-sdk/s3-request-presigner';
import { Env } from '../../config/env';

/** S3-совместимое хранилище (Cloudflare R2 / MinIO): presigned PUT + каскадное удаление префикса. */
@Injectable()
export class S3Service {
  private client(): S3Client {
    const cfg = Env.s3();
    return new S3Client({
      region: cfg.region,
      endpoint: cfg.endpoint || undefined,
      forcePathStyle: true, // MinIO/R2
      credentials: { accessKeyId: cfg.accessKeyId, secretAccessKey: cfg.secretAccessKey },
    });
  }

  /** Layout: users/{userId}/analyses/{sessionId}/{original|annotated}.mp4 (DECISIONS §7.2). */
  analysisKey(userId: string, sessionId: string, file: string): string {
    return `users/${userId}/analyses/${sessionId}/${file}`;
  }

  async presignPut(key: string, contentType: string, expiresInSec = 900): Promise<string> {
    const cmd = new PutObjectCommand({ Bucket: Env.s3().bucket, Key: key, ContentType: contentType });
    return getSignedUrl(this.client(), cmd, { expiresIn: expiresInSec });
  }

  async presignGet(key: string, expiresInSec = 3600): Promise<string> {
    const cmd = new GetObjectCommand({ Bucket: Env.s3().bucket, Key: key });
    return getSignedUrl(this.client(), cmd, { expiresIn: expiresInSec });
  }

  /** Каскадное удаление: все объекты под префиксом сессии. */
  async deletePrefix(prefix: string): Promise<number> {
    const client = this.client();
    const bucket = Env.s3().bucket;
    let deleted = 0;
    let token: string | undefined;
    do {
      const listed = await client.send(
        new ListObjectsV2Command({ Bucket: bucket, Prefix: prefix, ContinuationToken: token }),
      );
      const objects = (listed.Contents ?? []).map((o) => ({ Key: o.Key! }));
      if (objects.length > 0) {
        await client.send(new DeleteObjectsCommand({ Bucket: bucket, Delete: { Objects: objects } }));
        deleted += objects.length;
      }
      token = listed.IsTruncated ? listed.NextContinuationToken : undefined;
    } while (token);
    return deleted;
  }

  async getObjectBuffer(key: string): Promise<Buffer | null> {
    try {
      const res = await this.client().send(new GetObjectCommand({ Bucket: Env.s3().bucket, Key: key }));
      return Buffer.from(await res.Body!.transformToByteArray());
    } catch {
      return null;
    }
  }
}
