import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { Drill, DrillDocument, DrillSchema } from '../../common/schemas/drill.schema';
import { DrillsController } from './drills.controller';
import { DrillsService } from './drills.service';
import { DrillsSeedService } from './drills.seed';

@Module({
  imports: [MongooseModule.forFeature([{ name: Drill.name, schema: DrillSchema }])],
  controllers: [DrillsController],
  providers: [DrillsService, DrillsSeedService],
  exports: [DrillsService, DrillsSeedService],
})
export class DrillsModule {}
