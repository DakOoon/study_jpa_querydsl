package study.querydsl.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.entity.Member;
import study.querydsl.repository.support.Querydsl4RepositorySupport;

import java.util.List;

import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.team;

@Repository
public class MemberRepositorySupport extends Querydsl4RepositorySupport<Member> {
    public MemberRepositorySupport() {
        super(Member.class);
    }

    public List<Member> select() {
        return select(member)
                .from(member)
                .fetch();
    }

    public List<Member> selectFrom() {
        return selectFrom(member)
                .fetch();
    }

    public Page<Member> searchPageByApplyPage(MemberSearchCondition condition, Pageable pageable) {
        JPAQuery<Member> query = selectFrom(member)
                .leftJoin(member.team, team)
                .where(memberNameEq(condition.getUserName()),
                        teamNameEq(condition.getTeamName()),
                        memberAgeGoe(condition.getAgeGoe()),
                        memberAgeLoe(condition.getAgeLoe()));

        List<Member> content = getQuerydsl().applyPagination(pageable, query)
                .fetch();

        return PageableExecutionUtils.getPage(content, pageable, query::fetchCount);
    }

    public Page<Member> applyPagination(MemberSearchCondition condition, Pageable pageable) {
        return applyPagination(pageable, query -> query
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(memberNameEq(condition.getUserName()),
                        teamNameEq(condition.getTeamName()),
                        memberAgeGoe(condition.getAgeGoe()),
                        memberAgeLoe(condition.getAgeLoe())));
    }

    public Page<Member> applyPaginationWithCount(MemberSearchCondition condition, Pageable pageable) {
        return applyPagination(pageable,
                contentQuery -> contentQuery
                        .selectFrom(member)
                        .leftJoin(member.team, team)
                        .where(memberNameEq(condition.getUserName()),
                                teamNameEq(condition.getTeamName()),
                                memberAgeGoe(condition.getAgeGoe()),
                                memberAgeLoe(condition.getAgeLoe())),
                countQuery -> countQuery
                        .select(member.count())
                        .from(member)
                        .leftJoin(member.team, team)
                        .where(memberNameEq(condition.getUserName()),
                                teamNameEq(condition.getTeamName()),
                                memberAgeGoe(condition.getAgeGoe()),
                                memberAgeLoe(condition.getAgeLoe())));
    }

    private BooleanExpression memberNameEq(String userName) {
        return StringUtils.hasLength(userName) ? member.name.eq(userName) : null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return StringUtils.hasLength(teamName) ? team.name.eq(teamName) : null;
    }

    private BooleanExpression memberAgeGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression memberAgeLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }
}
