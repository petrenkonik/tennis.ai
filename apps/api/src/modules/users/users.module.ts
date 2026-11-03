import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { Analysis, AnalysisSchema } from '../../common/schemas/analysis.schema';
import { Subscription, SubscriptionSchema } from '../../common/schemas/subscription.schema';
import { User, UserSchema } from '../../common/schemas/user.schema';
import { UsersController } from './users.controller';
import { UsersService } from './users.service';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: User.name, schema: UserSchema },
      { name: Subscription.name, schema: SubscriptionSchema },
      { name: Analysis.name, schema: AnalysisSchema },
    ]),
  ],
  controllers: [UsersController],
  providers: [UsersService],
  exports: [UsersService],
})
export class UsersModule {}
