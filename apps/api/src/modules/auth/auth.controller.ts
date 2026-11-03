import { Body, Controller, Post } from '@nestjs/common';
import { Public } from '../../common/public.decorator';
import { AuthService } from './auth.service';
import { TelegramAuthDto } from './auth.dto';

@Controller('auth')
export class AuthController {
  constructor(private readonly auth: AuthService) {}

  /** Единственная точка входа: TMA шлёт initData, API отвечает JWT. */
  @Public()
  @Post('telegram')
  async telegram(@Body() dto: TelegramAuthDto) {
    return this.auth.login(dto);
  }
}
