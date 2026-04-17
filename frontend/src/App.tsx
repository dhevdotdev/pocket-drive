import { useState } from "react";
import { Show, SignIn, UserButton } from "@clerk/react";
import { UploadZone } from "./components/UploadZone";
import { FileList } from "./components/FileList";
import { Toaster } from "./components/Toaster";

export default function App() {
  const [refreshKey, setRefreshKey] = useState(0);

  return (
    <div className="min-h-screen bg-ctp-mantle text-ctp-text">
      <header className="sticky top-0 z-10 bg-ctp-base/80 backdrop-blur border-b border-ctp-surface0 px-6 py-3 flex items-center justify-between">
        <div className="flex items-center gap-2.5">
          <div className="w-7 h-7 rounded-lg bg-gradient-to-br from-ctp-mauve to-ctp-lavender flex items-center justify-center shadow-sm">
            <svg className="w-3.5 h-3.5 text-ctp-base" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M3 7a2 2 0 012-2h4l2 2h8a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V7z" />
            </svg>
          </div>
          <span className="font-semibold text-sm text-ctp-text tracking-tight">
            pocket<span className="text-ctp-mauve">drive</span>
          </span>
        </div>
        <Show when="signed-in">
          <UserButton />
        </Show>
        <Show when="signed-out">
          <div />
        </Show>
      </header>

      <main className="max-w-2xl mx-auto px-6 py-12 space-y-6">
        <Show when="signed-out">
          <div className="flex flex-col items-center gap-8 py-10">
            <div className="text-center space-y-2">
              <div className="flex justify-center mb-4">
                <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-ctp-mauve to-ctp-lavender flex items-center justify-center shadow-lg" style={{ boxShadow: `0 8px 32px color-mix(in srgb, var(--ctp-mauve) 25%, transparent)` }}>
                  <svg className="w-8 h-8 text-ctp-base" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M3 7a2 2 0 012-2h4l2 2h8a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V7z" />
                  </svg>
                </div>
              </div>
              <h1 className="text-2xl font-semibold tracking-tight text-ctp-text">
                pocket<span className="text-ctp-mauve">drive</span>
              </h1>
              <p className="text-sm text-ctp-subtext0">Your personal cloud drive.</p>
            </div>
            <SignIn routing="hash" />
          </div>
        </Show>

        <Show when="signed-in">
          <UploadZone onUploaded={() => setRefreshKey((k) => k + 1)} />
          <FileList refreshKey={refreshKey} />
        </Show>
      </main>

      <Toaster />
    </div>
  );
}
