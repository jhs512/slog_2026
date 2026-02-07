"use client";

import Link from "next/link";

import { useEffect, useState } from "react";

import withAdmin from "@/global/auth/hoc/withAdmin";
import type { components } from "@/global/backend/apiV1/schema";
import client from "@/global/backend/client";

type MemberWithUsernameDto = components["schemas"]["MemberWithUsernameDto"];

export default withAdmin(function Page() {
  const [members, setMembers] = useState<MemberWithUsernameDto[] | null>(null);

  useEffect(() => {
    client
      .GET("/member/api/v1/adm/members")
      .then((res) => res.data && setMembers(res.data.content));
  }, []);

  if (members == null) return <div>로딩중...</div>;

  return (
    <>
      <h1>회원 목록</h1>

      {members.length == 0 && <div>회원이 없습니다.</div>}

      {members.length > 0 && (
        <ul>
          {members.map((member) => (
            <li key={member.id}>
              <Link href={`./members/${member.id}`}>
                {member.id} : {member.username} / {member.name}
              </Link>
            </li>
          ))}
        </ul>
      )}
    </>
  );
});
