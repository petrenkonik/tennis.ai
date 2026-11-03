import { Global, Module } from '@nestjs/common';
import { JwtModule } from '@nestjs/jwt';
import { MongooseModule } from '@nestjs/mongoose';
import { Env } from '../../config/env';
import { User, UserSchema } from '../../common/schemas/user.schema';
import { AuthController } from './auth.controller';
import { AuthService } from './auth.service';

// global: true — JwtService is needed by the guard and the WS gateway throughout the app.
@Global()
@Module({
  imports: [
    MongooseModule.forFeature([{ name: User.name, schema: UserSchema }]),
    JwtModule.register({ global: true, secret: Env.jwtSecret(), signOptions: { expiresIn: '30d' } }),
  ],
  controllers: [AuthController],
  providers: [AuthService],
  exports: [JwtModule],
})
export class AuthModule {}
