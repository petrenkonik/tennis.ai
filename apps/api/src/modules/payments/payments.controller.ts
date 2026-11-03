import { BadRequestException, Body, Controller, Logger, Post, RawBodyRequest, Req } from '@nestjs/common';
import type { Request } from 'express';
import { IsIn } from 'class-validator';
import { CurrentUser, type JwtPayload } from '../../common/current-user.decorator';
import { Env } from '../../config/env';
import { PaymentsService } from './payments.service';

class InvoiceDto {
  @IsIn(['pro_1m', 'pro_3m', 'pro_12m'])
  productId!: 'pro_1m' | 'pro_3m' | 'pro_12m';
}

@Controller('payments')
export class PaymentsController {
  private readonly logger = new Logger(PaymentsController.name);

  constructor(private readonly payments: PaymentsService) {}

  @Post('invoice')
  async invoice(@CurrentUser() u: JwtPayload, @Body() dto: InvoiceDto) {
    return this.payments.createInvoice(u.sub, u.telegramId, dto.productId);
  }

  /** Вебхук бота (setWebhook с secret_token). Public: Telegram не имеет нашего JWT. */
  @Post('webhook')
  async webhook(@Req() req: RawBodyRequest<Request>) {
    const expected = Env.botWebhookSecret();
    const got = req.headers['x-telegram-bot-api-secret-token'];
    if (expected && got !== expected) {
      this.logger.warn('webhook rejected: secret token mismatch');
      throw new BadRequestException();
    }
    return this.payments.handleUpdate(req.body as object);
  }
}
