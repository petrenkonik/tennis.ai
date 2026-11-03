import { Body, Controller, Post } from '@nestjs/common';
import { randomUUID } from 'node:crypto';
import { CurrentUser, type JwtPayload } from '../../common/current-user.decorator';
import { S3Service } from './s3.service';
import { PresignDto } from './uploads.dto';

const FILE_BY_KIND: Record<string, string> = {
  original: 'original.mp4',
  frame: 'frame_0001.jpg',
  annotated: 'annotated.mp4',
};

@Controller('uploads')
export class UploadsController {
  constructor(private readonly s3: S3Service) {}

  /**
   * POST /api/uploads/presign — presigned PUT (DECISIONS §7.3).
   * The client uploads the file directly to S3-compatible storage; the API only issues keys.
   */
  @Post('presign')
  async presign(@Body() dto: PresignDto, @CurrentUser() u: JwtPayload) {
    const sessionId = dto.sessionId ?? randomUUID();
    const key = this.s3.analysisKey(u.sub, sessionId, FILE_BY_KIND[dto.kind] ?? 'file.bin');
    const uploadUrl = await this.s3.presignPut(key, dto.contentType);
    return { sessionId, key, uploadUrl, expiresInSec: 900 };
  }
}
