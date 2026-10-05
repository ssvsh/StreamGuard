export type FieldType =
  | 'STRING' | 'INTEGER' | 'LONG' | 'DOUBLE' | 'DECIMAL' | 'BOOLEAN';

export interface FieldDefinition {
  type: FieldType;
  required: boolean;
}

export interface QualityRule {
  field: string;
  operator: string;
  expectedValue: string;
}

export interface Contract {
  id: string;
  eventType: string;
  version: number;
  fields: Record<string, FieldDefinition>;
  qualityRules: QualityRule[];
  createdAt: string;
}

export type ChangeType = 'FIELD_ADDED' | 'FIELD_REMOVED' | 'TYPE_CHANGED' | 'REQUIRED_CHANGED';
export type Compatibility = 'COMPATIBLE' | 'POTENTIALLY_BREAKING' | 'BREAKING';

export interface SchemaChange {
  field: string;
  type: ChangeType;
  oldType: string | null;
  newType: string | null;
  oldRequired: boolean | null;
  newRequired: boolean | null;
}

export interface SchemaAnalysis {
  eventType: string;
  oldVersion: number;
  newVersion: number;
  compatibility: Compatibility;
  changes: SchemaChange[];
}

export interface Violation {
  id: string;
  eventType: string;
  schemaVersion: number;
  eventId: string;
  violationType: string;
  field: string;
  message: string;
  timestamp: string;
}

export interface MigrationRecommendation {
  severity: string;
  summary: string;
  migrationStrategy: string[];
  rollbackStrategy: string[];
  risks: string[];
}