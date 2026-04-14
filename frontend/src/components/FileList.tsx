import { useCallback, useEffect, useState } from "react";
import { useFilesApi } from "../hooks/useFilesApi";
import { useToast } from "../hooks/useToast";
import { getErrorMessage } from "../utils/error";
import type { FileItem } from "../api/files";

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

function FileIcon({ contentType }: { contentType: string }) {
  const cls = "w-4 h-4";
  if (contentType === "application/pdf")
    return <svg className={cls} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}><path strokeLinecap="round" strokeLinejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" /></svg>;
  if (contentType.startsWith("image/"))
    return <svg className={cls} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}><path strokeLinecap="round" strokeLinejoin="round" d="M2.25 15.75l5.159-5.159a2.25 2.25 0 013.182 0l5.159 5.159m-1.5-1.5l1.409-1.409a2.25 2.25 0 013.182 0l2.909 2.909m-18 3.75h16.5a1.5 1.5 0 001.5-1.5V6a1.5 1.5 0 00-1.5-1.5H3.75A1.5 1.5 0 002.25 6v12a1.5 1.5 0 001.5 1.5zm10.5-11.25h.008v.008h-.008V8.25zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0z" /></svg>;
  if (contentType === "application/zip")
    return <svg className={cls} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}><path strokeLinecap="round" strokeLinejoin="round" d="M20.25 7.5l-.625 10.632a2.25 2.25 0 01-2.247 2.118H6.622a2.25 2.25 0 01-2.247-2.118L3.75 7.5M10 11.25h4M3.375 7.5h17.25c.621 0 1.125-.504 1.125-1.125v-1.5c0-.621-.504-1.125-1.125-1.125H3.375c-.621 0-1.125.504-1.125 1.125v1.5c0 .621.504 1.125 1.125 1.125z" /></svg>;
  return <svg className={cls} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}><path strokeLinecap="round" strokeLinejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m2.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" /></svg>;
}

type DeleteState = { id: string; phase: "confirming" | "deleting" } | null;

interface Props {
  refreshKey: number;
}

export function FileList({ refreshKey }: Props) {
  const api = useFilesApi();
  const toast = useToast();
  const [files, setFiles] = useState<FileItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [deleteState, setDeleteState] = useState<DeleteState>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await api.listFiles();
      setFiles(res.content);
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }, [api]);

  useEffect(() => { load(); }, [load, refreshKey]);

  async function handleDownload(file: FileItem) {
    try {
      const { downloadUrl } = await api.getDownloadUrl(file.fileId);
      window.open(downloadUrl, "_blank");
    } catch (e) {
      toast.error(`Download failed: ${getErrorMessage(e)}`);
    }
  }

  async function handleDelete(fileId: string, filename: string) {
    setDeleteState({ id: fileId, phase: "deleting" });
    try {
      await api.deleteFile(fileId);
      setFiles((prev) => prev.filter((f) => f.fileId !== fileId));
      toast.success(`"${filename}" deleted`);
    } catch (e) {
      toast.error(`Delete failed: ${getErrorMessage(e)}`);
    } finally {
      setDeleteState(null);
    }
  }

  if (loading) {
    return (
      <div className="space-y-2">
        {[...Array(3)].map((_, i) => (
          <div key={i} className="h-[60px] rounded-xl bg-zinc-100 dark:bg-zinc-800/60 animate-pulse" />
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <div className="text-center py-10 space-y-3">
        <p className="text-sm text-red-500">{error}</p>
        <button
          onClick={load}
          className="text-xs px-3 py-1.5 rounded-lg bg-zinc-100 dark:bg-zinc-800 text-zinc-600 dark:text-zinc-300 hover:bg-zinc-200 dark:hover:bg-zinc-700 transition-colors"
        >
          Retry
        </button>
      </div>
    );
  }

  if (!files.length) {
    return (
      <div className="text-center py-16 space-y-1">
        <p className="text-sm font-medium text-zinc-500 dark:text-zinc-400">No files yet</p>
        <p className="text-xs text-zinc-400 dark:text-zinc-500">Upload your first file above</p>
      </div>
    );
  }

  return (
    <div>
      <p className="text-xs font-medium text-zinc-400 uppercase tracking-wider mb-3">
        {files.length} {files.length === 1 ? "file" : "files"}
      </p>
      <ul className="space-y-1.5">
        {files.map((file) => (
          <li
            key={file.fileId}
            className="flex items-center gap-3 px-4 py-3 rounded-xl bg-white dark:bg-zinc-900 border border-zinc-100 dark:border-zinc-800 hover:border-zinc-200 dark:hover:border-zinc-700 transition-colors shadow-sm"
          >
            <div className="w-8 h-8 rounded-lg bg-zinc-100 dark:bg-zinc-800 flex items-center justify-center shrink-0 text-zinc-400">
              <FileIcon contentType={file.contentType} />
            </div>
            <div className="min-w-0 flex-1">
              <p className="text-sm font-medium text-zinc-800 dark:text-zinc-100 truncate leading-tight">
                {file.originalName}
              </p>
              <p className="text-xs text-zinc-400 mt-0.5">
                {formatBytes(file.sizeBytes)} · {formatDate(file.createdAt)}
              </p>
            </div>

            {deleteState?.id === file.fileId && deleteState.phase === "confirming" ? (
              <div className="flex gap-1.5 shrink-0 items-center">
                <span className="text-xs text-zinc-500 mr-1">Delete?</span>
                <button
                  onClick={() => handleDelete(file.fileId, file.originalName)}
                  className="text-xs px-3 py-1.5 rounded-lg bg-red-500 hover:bg-red-600 text-white font-medium transition-colors"
                >
                  Yes
                </button>
                <button
                  onClick={() => setDeleteState(null)}
                  className="text-xs px-3 py-1.5 rounded-lg bg-zinc-100 dark:bg-zinc-800 text-zinc-600 dark:text-zinc-300 hover:bg-zinc-200 dark:hover:bg-zinc-700 font-medium transition-colors"
                >
                  Cancel
                </button>
              </div>
            ) : (
              <div className="flex gap-1.5 shrink-0">
                <button
                  onClick={() => handleDownload(file)}
                  aria-label={`Download ${file.originalName}`}
                  className="text-xs px-3 py-1.5 rounded-lg bg-zinc-100 dark:bg-zinc-800 hover:bg-violet-100 dark:hover:bg-violet-900/30 text-zinc-500 hover:text-violet-600 dark:hover:text-violet-400 font-medium transition-colors"
                >
                  Download
                </button>
                <button
                  onClick={() => setDeleteState({ id: file.fileId, phase: "confirming" })}
                  disabled={deleteState?.id === file.fileId && deleteState.phase === "deleting"}
                  aria-label={`Delete ${file.originalName}`}
                  className="text-xs px-3 py-1.5 rounded-lg bg-zinc-100 dark:bg-zinc-800 hover:bg-red-100 dark:hover:bg-red-900/20 text-zinc-500 hover:text-red-500 font-medium disabled:opacity-40 transition-colors"
                >
                  {deleteState?.id === file.fileId && deleteState.phase === "deleting" ? "…" : "Delete"}
                </button>
              </div>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}
