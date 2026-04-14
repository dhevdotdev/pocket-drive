import { useMemo } from "react";
import { useAuth } from "@clerk/react";
import { createFilesApi } from "../api/files";

export function useFilesApi() {
  const { getToken } = useAuth();
  return useMemo(() => createFilesApi(getToken), [getToken]);
}
