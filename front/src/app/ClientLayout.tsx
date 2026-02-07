"use client";

import Link from "next/link";

import NewPostNotification from "@/domain/post/components/NewPostNotification";
import { useAuthContext } from "@/global/auth/hooks/useAuth";

import NarrowHeaderContent from "@/lib/business/components/NarrowHeaderContent";
import WideHeaderContent from "@/lib/business/components/WideHeaderContent";

import { Button } from "@/components/ui/button";

import { Copyright, LogIn } from "lucide-react";

export default function ClientLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const { isPending } = useAuthContext();

  if (isPending) {
    return (
      <div className="flex-1 flex justify-center items-center text-muted-foreground">
        로딩중...
      </div>
    );
  }

  return (
    <>
      <header className="sticky top-0 z-50 border-b border-border bg-background/80 backdrop-blur-sm">
        <NarrowHeaderContent className="flex sm:hidden" />
        <WideHeaderContent className="hidden sm:flex" />
      </header>
      <main className="flex-1 flex flex-col bg-background">{children}</main>
      <footer className="border-t border-border bg-card">
        <div className="container mx-auto px-4 py-4 flex justify-center gap-4">
          <Button variant="link" asChild>
            <Link href="/">
              <Copyright /> 2026 슬로그
            </Link>
          </Button>
          <Button variant="link" asChild>
            <Link href="/adm/members/login">
              <LogIn /> 관리자
            </Link>
          </Button>
        </div>
      </footer>
      <NewPostNotification />
    </>
  );
}
