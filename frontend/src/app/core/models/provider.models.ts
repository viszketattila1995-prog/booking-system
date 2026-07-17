export type ProviderStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'SUSPENDED';

export interface ProviderResponse {
  id: string;
  organizationId: string;
  organizationName: string;
  status: ProviderStatus;
  appliedAt: string;
  decidedAt: string | null;
}

export interface ApplyProviderRequest {
  organizationName: string;
  organizationDescription?: string;
}
