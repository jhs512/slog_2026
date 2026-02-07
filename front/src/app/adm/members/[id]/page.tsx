"use client";

import Link from "next/link";

import { use, useEffect, useState } from "react";

import withAdmin from "@/global/auth/hoc/withAdmin";
import type { components } from "@/global/backend/apiV1/schema";
import client from "@/global/backend/client";

type MemberWithUsernameDto = components["schemas"]["MemberWithUsernameDto"];

export default withAdmin(function Page({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id: idStr } = use(params);
  const id = parseInt(idStr);

  const [member, setMember] = useState<MemberWithUsernameDto | null>(null);

  useEffect(() => {
    client
      .GET("/member/api/v1/adm/members/{id}", {
        params: {
          path: {
            id,
          },
        },
      })
      .then((res) => res.data && setMember(res.data));
  }, [id]);

  if (member == null) return <div>로딩중...</div>;

  return (
    <>
      <h1>회원 정보</h1>

      <div>
        <img
          src={member.profileImageUrl}
          alt={`${member.name} 프로필`}
          style={{ width: 100, height: 100, borderRadius: "50%" }}
        />
      </div>

      <table>
        <tbody>
          <tr>
            <th>ID</th>
            <td>{member.id}</td>
          </tr>
          <tr>
            <th>아이디</th>
            <td>{member.username}</td>
          </tr>
          <tr>
            <th>이름</th>
            <td>{member.name}</td>
          </tr>
          <tr>
            <th>관리자 여부</th>
            <td>{member.isAdmin ? "예" : "아니오"}</td>
          </tr>
          <tr>
            <th>가입일</th>
            <td>{new Date(member.createdAt).toLocaleString()}</td>
          </tr>
          <tr>
            <th>수정일</th>
            <td>{new Date(member.modifiedAt).toLocaleString()}</td>
          </tr>
        </tbody>
      </table>

      <div style={{ marginTop: 20 }}>
        <Link href="/adm/members">목록으로</Link>
      </div>
    </>
  );
});
