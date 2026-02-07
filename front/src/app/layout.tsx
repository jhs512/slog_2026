import { ThemeProvider } from "next-themes";

import { Geist_Mono } from "next/font/google";

import type { Metadata } from "next";

import "./globals.css";

import { Toaster } from "@/components/ui/sonner";

import ContextLayout from "./ContextLayout";

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "슬로그",
  description: "슬로그는 당신을 위한 기술 블로그 입니다.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko" suppressHydrationWarning>
      <body
        className={`${geistMono.variable} antialiased min-h-screen flex flex-col`}
      >
        <ThemeProvider
          attribute="class"
          defaultTheme="system"
          enableSystem
          disableTransitionOnChange
        >
          <ContextLayout>{children}</ContextLayout>
          <Toaster richColors position="top-center" />
        </ThemeProvider>
      </body>
    </html>
  );
}
