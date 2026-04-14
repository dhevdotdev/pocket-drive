import { useRef, useState } from "react";
import { useUpload } from "../hooks/useUpload";
import { cn } from "../utils/cn";

interface Props {
  onUploaded: () => void;
}

export function UploadZone({ onUploaded }: Props) {
  const { state, upload, reset } = useUpload(onUploaded);
  const inputRef = useRef<HTMLInputElement>(null);
  const [dragging, setDragging] = useState(false);

  function handleFiles(files: FileList | null) {
    if (!files?.length) return;
    upload(files[0]);
  }

  if (state.status === "uploading" || state.status === "confirming") {
    const pct = state.status === "uploading" ? state.progress : 100;
    const label = state.status === "confirming" ? "Confirming…" : `Uploading ${pct}%`;
    return (
      <div className="rounded-xl bg-white dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 p-5 space-y-3 shadow-sm">
        <div className="flex justify-between items-center">
          <p className="text-sm font-medium text-zinc-700 dark:text-zinc-300 truncate max-w-[70%]">
            {state.filename}
          </p>
          <span className="text-xs text-zinc-400 tabular-nums shrink-0">{label}</span>
        </div>
        <div className="h-1.5 w-full rounded-full bg-zinc-100 dark:bg-zinc-800 overflow-hidden">
          <div
            className="h-full bg-violet-500 rounded-full transition-all duration-200 ease-out"
            style={{ width: `${pct}%` }}
          />
        </div>
      </div>
    );
  }

  if (state.status === "error") {
    return (
      <div className="rounded-xl border border-red-200 dark:border-red-800 bg-red-50 dark:bg-red-950/40 p-5 flex items-start gap-3">
        <svg className="w-4 h-4 text-red-500 mt-0.5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <div className="flex-1 min-w-0">
          <p className="text-sm text-red-600 dark:text-red-400">{state.message}</p>
          <button onClick={reset} className="mt-1 text-xs text-red-500 underline underline-offset-2 hover:text-red-700">
            Try again
          </button>
        </div>
      </div>
    );
  }

  return (
    <div
      role="button"
      tabIndex={0}
      aria-label="Upload file — click or drag and drop"
      onClick={() => inputRef.current?.click()}
      onKeyDown={(e) => e.key === "Enter" && inputRef.current?.click()}
      onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
      onDragLeave={() => setDragging(false)}
      onDrop={(e) => { e.preventDefault(); setDragging(false); handleFiles(e.dataTransfer.files); }}
      className={cn(
        "rounded-xl border-2 border-dashed p-12 text-center cursor-pointer transition-all duration-150 select-none outline-none focus-visible:ring-2 focus-visible:ring-violet-400",
        dragging
          ? "border-violet-400 bg-violet-50 dark:bg-violet-950/20 scale-[0.99]"
          : "border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 hover:border-violet-300 dark:hover:border-violet-700 hover:bg-violet-50/50 dark:hover:bg-violet-950/10"
      )}
    >
      <input ref={inputRef} type="file" className="hidden" onChange={(e) => handleFiles(e.target.files)} />
      <div className="flex flex-col items-center gap-3">
        <div className={cn(
          "w-11 h-11 rounded-xl flex items-center justify-center transition-colors",
          dragging ? "bg-violet-100 dark:bg-violet-900/40" : "bg-zinc-100 dark:bg-zinc-800"
        )}>
          <svg className={cn("w-5 h-5 transition-colors", dragging ? "text-violet-500" : "text-zinc-400")} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5m-13.5-9L12 3m0 0l4.5 4.5M12 3v13.5" />
          </svg>
        </div>
        <div>
          <p className="text-sm font-medium text-zinc-700 dark:text-zinc-300">
            Drop a file or <span className="text-violet-500">browse</span>
          </p>
          <p className="text-xs text-zinc-400 mt-0.5">PDF, PNG, JPG, GIF, TXT, ZIP · max 100 MB</p>
        </div>
      </div>
    </div>
  );
}
