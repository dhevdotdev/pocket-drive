import { useEffect, useRef, useState } from "react";
import { useFilesApi } from "./useFilesApi";
import { useToast } from "./useToast";
import { getErrorMessage } from "../utils/error";

export type UploadState =
  | { status: "idle" }
  | { status: "uploading"; progress: number; filename: string }
  | { status: "confirming"; filename: string }
  | { status: "done" }
  | { status: "error"; message: string };

export function useUpload(onDone: () => void) {
  const api = useFilesApi();
  const toast = useToast();
  const [state, setState] = useState<UploadState>({ status: "idle" });
  const abortRef = useRef<AbortController | null>(null);

  useEffect(() => {
    return () => abortRef.current?.abort();
  }, []);

  async function upload(file: File) {
    abortRef.current = new AbortController();
    setState({ status: "uploading", progress: 0, filename: file.name });
    try {
      const { fileId, uploadUrl } = await api.initiateUpload(file.name, file.type, file.size);
      await api.uploadToR2(uploadUrl, file, (pct) =>
        setState({ status: "uploading", progress: pct, filename: file.name }),
        abortRef.current!.signal
      );
      setState({ status: "confirming", filename: file.name });
      await api.confirmUpload(fileId);
      setState({ status: "done" });
      toast.success(`"${file.name}" uploaded`);
      onDone();
    } catch (e) {
      setState({ status: "error", message: getErrorMessage(e) });
    }
  }

  function reset() {
    abortRef.current?.abort();
    setState({ status: "idle" });
  }

  return { state, upload, reset };
}
