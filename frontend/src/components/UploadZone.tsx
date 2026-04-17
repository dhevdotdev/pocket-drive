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
      <div className="rounded-xl bg-ctp-base border border-ctp-surface0 p-5 space-y-3 shadow-sm">
        <div className="flex justify-between items-center">
          <p className="text-sm font-medium text-ctp-subtext1 truncate max-w-[70%]">
            {state.filename}
          </p>
          <span className="text-xs text-ctp-overlay1 tabular-nums shrink-0">{label}</span>
        </div>
        <div className="h-1.5 w-full rounded-full bg-ctp-surface0 overflow-hidden">
          <div
            className="h-full bg-ctp-mauve rounded-full transition-all duration-200 ease-out"
            style={{ width: `${pct}%` }}
          />
        </div>
      </div>
    );
  }

  if (state.status === "error") {
    return (
      <div className="rounded-xl border border-ctp-red/30 bg-ctp-red/10 p-5 flex items-start gap-3">
        <svg className="w-4 h-4 text-ctp-red mt-0.5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <div className="flex-1 min-w-0">
          <p className="text-sm text-ctp-red">{state.message}</p>
          <button onClick={reset} className="mt-1 text-xs text-ctp-red underline underline-offset-2 hover:opacity-80">
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
        "rounded-xl border-2 border-dashed p-12 text-center cursor-pointer transition-all duration-150 select-none outline-none focus-visible:ring-2 focus-visible:ring-ctp-mauve",
        dragging
          ? "border-ctp-mauve bg-ctp-mauve/10 scale-[0.99]"
          : "border-ctp-surface1 bg-ctp-base hover:border-ctp-mauve/50 hover:bg-ctp-mauve/5"
      )}
    >
      <input ref={inputRef} type="file" className="hidden" onChange={(e) => handleFiles(e.target.files)} />
      <div className="flex flex-col items-center gap-3">
        <div className={cn(
          "w-11 h-11 rounded-xl flex items-center justify-center transition-colors",
          dragging ? "bg-ctp-mauve/20" : "bg-ctp-surface0"
        )}>
          <svg className={cn("w-5 h-5 transition-colors", dragging ? "text-ctp-mauve" : "text-ctp-overlay1")} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5m-13.5-9L12 3m0 0l4.5 4.5M12 3v13.5" />
          </svg>
        </div>
        <div>
          <p className="text-sm font-medium text-ctp-subtext1">
            Drop a file or <span className="text-ctp-mauve">browse</span>
          </p>
          <p className="text-xs text-ctp-overlay1 mt-0.5">PDF, PNG, JPG, GIF, TXT, ZIP · max 100 MB</p>
        </div>
      </div>
    </div>
  );
}
