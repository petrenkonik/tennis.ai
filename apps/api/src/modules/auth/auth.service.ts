import { Injectable, UnauthorizedException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { validateInitData } from '@tennis-ai/telegram';
import { Env } from '../../config/env';
import { User, UserDocument } from '../../common/schemas/user.schema';
import type { JwtPayload } from '../../common/current-user.decorator';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { TelegramAuthDto } from './auth.dto';

@Injectable()
export class AuthService {
  constructor(
    private readonly jwt: JwtService,
    @InjectModel(User.name) private readonly users: Model<UserDocument>,
  ) {}

  /** initData → HMAC verification with the bot token → user upsert → JWT. */
  async login(dto: TelegramAuthDto): Promise<{ accessToken: string; user: UserDocument }> {
    const validation = validateInitData(dto.initData, Env.botToken());
    if (!validation.ok || !validation.user) {
      throw new UnauthorizedException(`initData invalid: ${validation.reason}`);
    }
    const tmaUser = validation.user;
    const user = await this.users.findOneAndUpdate(
      { telegramId: tmaUser.id },
      {
        $set: {
          firstName: tmaUser.first_name,
          username: tmaUser.username,
          photoUrl: tmaUser.photo_url,
        },
        $setOnInsert: { telegramId: tmaUser.id, language: tmaUser.language_code ?? 'en' },
      },
      { upsert: true, new: true },
    );

    const payload: JwtPayload = { sub: String(user._id), telegramId: user.telegramId };
    return { accessToken: this.jwt.sign(payload), user };
  }
}
