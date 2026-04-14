export type FileStatus = "PENDING" | "UPLOADED" | "FAILED" | "DELETED";

export interface FileItem {
  fileId: string;
  originalName: string;
  contentType: string;
  sizeBytes: number;
  status: FileStatus;
  createdAt: string;
}

export interface FileListResponse {
  content: FileItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface UploadInitiateResponse {
  fileId: string;
  uploadUrl: string;
  expiresIn: number;
}

export interface DownloadResponse {
  downloadUrl: string;
  expiresIn: number;
}

async function authFetch(
  getToken: () => Promise<string | null>,
  input: RequestInfo,
  init?: RequestInit
): Promise<Response> {
  const token = await getToken();
  const res = await fetch(input, {
    ...init,
    headers: {
      ...init?.headers,
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.message ?? `HTTP ${res.status}`);
  }
  return res;
}

export function createFilesApi(getToken: () => Promise<string | null>) {
  async function initiateUpload(
    filename: string,
    contentType: string,
    sizeBytes: number
  ): Promise<UploadInitiateResponse> {
    const res = await authFetch(getToken, "/api/v1/files/upload", {
      method: "POST",
      body: JSON.stringify({ filename, contentType, sizeBytes }),
    });
    return res.json();
  }

  function uploadToR2(
    uploadUrl: string,
    file: File,
    onProgress: (pct: number) => void,
    signal?: AbortSignal
  ): Promise<void> {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      xhr.open("PUT", uploadUrl);
      xhr.setRequestHeader("Content-Type", file.type);
      xhr.upload.onprogress = (e) => {
        if (e.lengthComputable) onProgress(Math.round((e.loaded / e.total) * 100));
      };
      xhr.onload = () =>
        xhr.status >= 200 && xhr.status < 300
          ? resolve()
          : reject(new Error(`R2 upload failed: ${xhr.status}`));
      xhr.onerror = () => reject(new Error("R2 upload network error"));
      xhr.onabort = () => reject(new Error("Upload cancelled"));
      signal?.addEventListener("abort", () => xhr.abort());
      xhr.send(file);
    });
  }

  async function confirmUpload(fileId: string): Promise<void> {
    await authFetch(getToken, `/api/v1/files/${fileId}/confirm`, { method: "POST" });
  }

  async function listFiles(page = 0): Promise<FileListResponse> {
    const res = await authFetch(getToken, `/api/v1/files?page=${page}&size=20`);
    return res.json();
  }

  async function getDownloadUrl(fileId: string): Promise<DownloadResponse> {
    const res = await authFetch(getToken, `/api/v1/files/${fileId}/download`);
    return res.json();
  }

  async function deleteFile(fileId: string): Promise<void> {
    await authFetch(getToken, `/api/v1/files/${fileId}`, { method: "DELETE" });
  }

  return { initiateUpload, uploadToR2, confirmUpload, listFiles, getDownloadUrl, deleteFile };
}
