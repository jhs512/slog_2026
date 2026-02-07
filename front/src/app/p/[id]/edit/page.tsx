"use client";

import { useRouter } from "next/navigation";

import { use, useEffect } from "react";

import usePost from "@/domain/post/hooks/usePost";
import withLogin from "@/global/auth/hoc/withLogin";
import client from "@/global/backend/client";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { z } from "zod";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";

import { Eye, FileEdit, List, Pencil, Save } from "lucide-react";

// 임시저장은 빈 값 허용, 공개 시에만 필수 체크
const postFormSchema = z
  .object({
    title: z.string().max(100, "제목은 100자 이하여야 합니다."),
    content: z.string(),
    published: z.boolean(),
    listed: z.boolean(),
  })
  .refine(
    (data) => {
      // 공개(published)일 때만 제목 필수
      if (data.published) {
        return data.title.trim().length >= 2;
      }
      return true;
    },
    {
      message: "공개 글의 제목은 2자 이상이어야 합니다.",
      path: ["title"],
    },
  )
  .refine(
    (data) => {
      // 공개(published)일 때만 내용 필수
      if (data.published) {
        return data.content.trim().length >= 2;
      }
      return true;
    },
    {
      message: "공개 글의 내용은 2자 이상이어야 합니다.",
      path: ["content"],
    },
  );

type PostFormInputs = z.infer<typeof postFormSchema>;

export default withLogin(function Page({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const router = useRouter();

  const { id: idStr } = use(params);
  const id = parseInt(idStr);

  const { post, setPost } = usePost(id);

  const form = useForm<PostFormInputs>({
    resolver: zodResolver(postFormSchema),
    defaultValues: {
      title: "",
      content: "",
      published: false,
      listed: false,
    },
  });

  const published = form.watch("published");

  useEffect(() => {
    if (post) {
      form.reset({
        title: post.title,
        content: post.content,
        published: post.published,
        listed: post.listed,
      });
    }
  }, [post, form]);

  if (post == null)
    return (
      <div className="flex items-center justify-center min-h-[50vh]">
        <div className="text-muted-foreground">로딩중...</div>
      </div>
    );

  const onSubmit = async (data: PostFormInputs) => {
    const response = await client.PUT("/post/api/v1/posts/{id}", {
      params: { path: { id } },
      body: {
        title: data.title,
        content: data.content,
        published: data.published,
        listed: data.listed,
      },
    });

    if (response.error) {
      toast.error(response.error.msg);
      return;
    }

    // 상태 업데이트
    setPost({
      ...post,
      title: data.title,
      content: data.content,
      published: data.published,
      listed: data.listed,
    });

    toast.success(response.data.msg, {
      action: {
        label: "글 보기",
        onClick: () => router.push(`/p/${id}`),
      },
    });
  };

  const isTemp = !post.published && post.title === "";

  return (
    <div className="container mx-auto px-4 py-8">
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-xl">
            <Pencil className="w-5 h-5" />
            {isTemp ? "새 글 작성" : "글 수정"}
            <Badge variant="outline" className="ml-auto">
              #{id}
            </Badge>
            {!published && (
              <Badge variant="secondary" className="ml-2">
                임시저장
              </Badge>
            )}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
              <FormField
                control={form.control}
                name="title"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="flex items-center gap-2">
                      <FileEdit className="w-4 h-4" />
                      제목
                      {!published && (
                        <span className="text-muted-foreground text-xs">
                          (임시저장은 선택)
                        </span>
                      )}
                    </FormLabel>
                    <FormControl>
                      <Input
                        {...field}
                        placeholder="제목을 입력하세요"
                        autoFocus
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <div className="flex items-center gap-6">
                <FormField
                  control={form.control}
                  name="published"
                  render={({ field }) => (
                    <div className="flex items-center gap-2">
                      <Checkbox
                        id="published"
                        checked={field.value}
                        onCheckedChange={(checked) => {
                          field.onChange(checked);
                          if (!checked) form.setValue("listed", false);
                        }}
                      />
                      <Label
                        htmlFor="published"
                        className="flex items-center gap-1 cursor-pointer"
                      >
                        <Eye className="w-4 h-4" />
                        공개
                      </Label>
                    </div>
                  )}
                />

                <FormField
                  control={form.control}
                  name="listed"
                  render={({ field }) => (
                    <div className="flex items-center gap-2">
                      <Checkbox
                        id="listed"
                        checked={field.value}
                        onCheckedChange={field.onChange}
                        disabled={!published}
                      />
                      <Label
                        htmlFor="listed"
                        className={`flex items-center gap-1 cursor-pointer ${!published ? "opacity-50" : ""}`}
                      >
                        <List className="w-4 h-4" />
                        목록 노출
                      </Label>
                    </div>
                  )}
                />

                <Button
                  type="submit"
                  variant="outline"
                  size="default"
                  disabled={form.formState.isSubmitting}
                >
                  <Save className="w-4 h-4" />
                  {form.formState.isSubmitting ? "저장 중..." : "저장"}
                </Button>
              </div>

              <FormField
                control={form.control}
                name="content"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>
                      내용
                      {!published && (
                        <span className="text-muted-foreground text-xs ml-2">
                          (임시저장은 선택)
                        </span>
                      )}
                    </FormLabel>
                    <FormControl>
                      <Textarea
                        {...field}
                        placeholder="내용을 입력하세요"
                        className="h-[calc(100dvh-400px)] min-h-[300px] max-h-[800px] resize-y"
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </form>
          </Form>
        </CardContent>
      </Card>

    </div>
  );
});
