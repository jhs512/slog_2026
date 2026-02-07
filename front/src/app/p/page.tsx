"use client";

import Image from "next/image";
import Link from "next/link";

import { useEffect, useState } from "react";

import PostWriteButton from "@/domain/post/components/PostWriteButton";
import type { components } from "@/global/backend/apiV1/schema";
import client from "@/global/backend/client";

import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

import { Eye, Heart, ListX, Lock, MessageCircle, Search } from "lucide-react";

type PostDto = components["schemas"]["PostDto"];

function formatDate(dateString: string): string {
  const date = new Date(dateString);
  return date.toLocaleDateString("ko-KR", {
    year: "2-digit",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "";

function getImageUrl(path: string | undefined): string {
  if (!path) return "";
  if (path.startsWith("http")) return path;
  return `${API_BASE_URL}${path}`;
}

export default function Page() {
  const [posts, setPosts] = useState<PostDto[] | null>(null);
  const [totalItems, setTotalItems] = useState(0);

  useEffect(() => {
    client.GET("/post/api/v1/posts").then((res) => {
      if (res.data) {
        setPosts(res.data.content);
        setTotalItems(
          res.data.pageable?.totalElements ?? res.data.content.length,
        );
      }
    });
  }, []);

  if (posts == null)
    return (
      <div className="flex items-center justify-center min-h-[50vh]">
        <div className="text-muted-foreground">로딩중...</div>
      </div>
    );

  return (
    <div className="container mx-auto px-4 py-6">
      <h1 className="text-2xl font-bold text-center my-4">공개글</h1>

      <div className="flex items-center justify-between mb-6">
        <span className="text-sm text-muted-foreground">총 {totalItems}개</span>
        <PostWriteButton />
      </div>

      {posts.length === 0 ? (
        <div className="flex flex-col items-center justify-center min-h-[calc(100vh-280px)] py-12 text-muted-foreground">
          <Search className="w-12 h-12 mb-4" />
          <p>글이 없습니다.</p>
        </div>
      ) : (
        <ul className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {posts.map((post) => (
            <li key={post.id}>
              <Link href={`/p/${post.id}`}>
                <Card className="hover:bg-accent/50 transition-colors">
                  <CardHeader className="pb-2">
                    <CardTitle className="flex items-center gap-2 break-all text-base">
                      <Badge variant="outline">{post.id}</Badge>
                      <span className="flex-1">{post.title}</span>
                      {!post.published && (
                        <Lock className="w-4 h-4 flex-shrink-0 text-muted-foreground" />
                      )}
                      {post.published && !post.listed && (
                        <ListX className="w-4 h-4 flex-shrink-0 text-muted-foreground" />
                      )}
                    </CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="flex items-center gap-3">
                      <Image
                        src={
                          getImageUrl(post.authorProfileImgUrl) ||
                          "/default-avatar.png"
                        }
                        alt={post.authorName}
                        width={40}
                        height={40}
                        className="w-10 h-10 rounded-full object-cover ring-2 ring-primary/10"
                        unoptimized
                      />
                      <div className="flex-1 min-w-0">
                        <div className="text-sm font-medium">
                          {post.authorName}
                        </div>
                        <div className="text-sm text-muted-foreground">
                          {formatDate(post.createdAt)}
                        </div>
                      </div>
                    </div>
                    <div className="flex items-center gap-4 mt-3 pt-3 border-t text-sm text-muted-foreground">
                      <div className="flex items-center gap-1">
                        <Eye className="w-4 h-4" />
                        <span>{post.hitCount}</span>
                      </div>
                      <div className="flex items-center gap-1">
                        <Heart
                          className={`w-4 h-4 ${post.actorHasLiked ? "fill-red-500 text-red-500" : ""}`}
                        />
                        <span>{post.likesCount}</span>
                      </div>
                      <div className="flex items-center gap-1">
                        <MessageCircle className="w-4 h-4" />
                        <span>{post.commentsCount}</span>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
