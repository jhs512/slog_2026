"use client";

import { useRouter } from "next/navigation";

import { useEffect, useState } from "react";

import PostWriteButton from "@/domain/post/components/PostWriteButton";
import withLogin from "@/global/auth/hoc/withLogin";
import type { components } from "@/global/backend/apiV1/schema";
import client from "@/global/backend/client";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";

import { Eye, List, Lock, Pencil, Search } from "lucide-react";

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

export default withLogin(function Page() {
  const router = useRouter();
  const [posts, setPosts] = useState<PostDto[] | null>(null);

  useEffect(() => {
    client
      .GET("/post/api/v1/posts/mine")
      .then((res) => res.data && setPosts(res.data.content));
  }, []);

  if (posts == null)
    return (
      <div className="flex items-center justify-center min-h-[50vh]">
        <div className="text-muted-foreground">로딩중...</div>
      </div>
    );

  const getStatusBadge = (post: PostDto) => {
    if (!post.published) {
      return (
        <Badge variant="secondary" className="gap-1">
          <Lock className="w-3 h-3" />
          임시저장
        </Badge>
      );
    }
    if (!post.listed) {
      return (
        <Badge variant="outline" className="gap-1">
          <Eye className="w-3 h-3" />
          비공개
        </Badge>
      );
    }
    return (
      <Badge variant="outline" className="gap-1">
        <List className="w-3 h-3" />
        공개
      </Badge>
    );
  };

  return (
    <div className="container mx-auto px-4 py-6">
      <h1 className="text-2xl font-bold text-center my-4">내 게시물</h1>

      <div className="flex items-center justify-between mb-6">
        <span className="text-sm text-muted-foreground">
          총 {posts.length}개
        </span>
        <PostWriteButton />
      </div>

      {posts.length === 0 ? (
        <div className="flex flex-col items-center justify-center min-h-[calc(100vh-280px)] py-12 text-muted-foreground">
          <Search className="w-12 h-12 mb-4" />
          <p>작성한 글이 없습니다.</p>
          <PostWriteButton className="mt-4" text="첫 글 작성하기" />
        </div>
      ) : (
        <ul className="space-y-3">
          {posts.map((post) => (
            <li key={post.id}>
              <Card
                className="hover:bg-accent/50 transition-colors cursor-pointer"
                onClick={() => router.push(`/p/${post.id}`)}
              >
                <CardContent className="p-4">
                  <div className="flex items-center gap-3">
                    <div className="flex-shrink-0">
                      <Badge variant="outline">{post.id}</Badge>
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 mb-1">
                        {getStatusBadge(post)}
                      </div>
                      <span className="font-medium block truncate">
                        {post.title || "(제목 없음)"}
                      </span>
                      <p className="text-sm text-muted-foreground mt-1">
                        {formatDate(post.createdAt)}
                      </p>
                    </div>
                    <div className="flex-shrink-0">
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={(e) => {
                          e.stopPropagation();
                          router.push(`/p/${post.id}/edit`);
                        }}
                      >
                        <Pencil className="w-4 h-4" />
                      </Button>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
});
