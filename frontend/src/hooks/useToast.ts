import { useToastContext } from "../context/ToastContext";

export function useToast() {
  const { addToast } = useToastContext();
  return {
    success: (message: string) => addToast("success", message),
    error: (message: string) => addToast("error", message),
  };
}
