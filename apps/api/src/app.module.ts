import { Module } from '@nestjs/common';
import { APP_GUARD } from '@nestjs/core';
import { MongooseModule } from '@nestjs/mongoose';
import { Env } from './config/env';
import { JwtAuthGuard } from './common/jwt-auth.guard';
import { AuthModule } from './modules/auth/auth.module';
import { UsersModule } from './modules/users/users.module';
import { UploadsModule } from './modules/uploads/uploads.module';
import { AnalysesModule } from './modules/analyses/analyses.module';
import { PaymentsModule } from './modules/payments/payments.module';
import { DrillsModule } from './modules/drills/drills.module';
import { PlansModule } from './modules/plans/plans.module';
import { CoachModule } from './modules/coach/coach.module';
import { HealthModule } from './modules/health/health.module';
import { WsModule } from './ws/ws.module';

@Module({
  imports: [
    MongooseModule.forRoot(Env.mongoUri()),
    AuthModule,
    UsersModule,
    UploadsModule,
    AnalysesModule,
    PaymentsModule,
    DrillsModule,
    PlansModule,
    CoachModule,
    HealthModule,
    WsModule,
  ],
  providers: [{ provide: APP_GUARD, useClass: JwtAuthGuard }],
})
export class AppModule {}
