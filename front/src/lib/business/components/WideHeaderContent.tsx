"use client";

import Link from "next/link";

import { useAuthContext } from "@/global/auth/hooks/useAuth";

import { Button } from "@/components/ui/button";

import { NotebookText, TableOfContents } from "lucide-react";

import LoginButton from "./LoginButton";
import Logo from "./Logo";
import MeMenuButton from "./MeMenuButton";
import PostWriteButton from "./PostWriteButton";
import ThemeToggleButton from "./ThemeToggleButton";

export default function WideHeaderContent({
  className,
}: {
  className?: string;
}) {
  const { isLogin } = useAuthContext();

  return (
    <div className={`${className} container mx-auto px-4 py-1`}>
      <Button variant="link" asChild>
        <Logo text />
      </Button>
      <Button variant="link" asChild>
        <Link href="/p">
          <TableOfContents /> 글
        </Link>
      </Button>
      {isLogin && <PostWriteButton text />}
      {isLogin && (
        <Button variant="link" asChild>
          <Link href="/p/mine">
            <NotebookText /> 내글
          </Link>
        </Button>
      )}

      <div className="flex-grow" />

      {!isLogin && <LoginButton />}
      {isLogin && <MeMenuButton />}
      <ThemeToggleButton />
    </div>
  );
}
