export type ApiResponse<T = unknown> = {
  success: boolean;
  message: string;
  data: T | null;
  errors: unknown;
};

export function success<T>(message: string, data: T): ApiResponse<T> {
  return { success: true, message, data, errors: null };
}

export function failure(message: string, errors: unknown = null): ApiResponse<null> {
  return { success: false, message, data: null, errors };
}
