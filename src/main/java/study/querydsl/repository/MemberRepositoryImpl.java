package study.querydsl.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;

import java.util.List;

import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.name.as("userName"),
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(memberNameEq(condition.getUserName()),
                        teamNameEq(condition.getTeamName()),
                        memberAgeGoe(condition.getAgeGoe()),
                        memberAgeLoe(condition.getAgeLoe()))
                .fetch();
    }

    @Override
    public Page<MemberTeamDto> searchPage(MemberSearchCondition condition, Pageable pageable) {
        List<MemberTeamDto> content = queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.name.as("userName"),
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(memberNameEq(condition.getUserName()),
                        teamNameEq(condition.getTeamName()),
                        memberAgeGoe(condition.getAgeGoe()),
                        memberAgeLoe(condition.getAgeLoe()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

//        Long total = queryFactory
//                .select(member.count())
//                .from(member)
//                .leftJoin(member.team, team)
//                .where(memberNameEq(condition.getUserName()),
//                        teamNameEq(condition.getTeamName()),
//                        memberAgeGoe(condition.getAgeGoe()),
//                        memberAgeLoe(condition.getAgeLoe()))
//                .fetchOne();
//
//        return new PageImpl<>(content, pageable, total);

        JPAQuery<Long> countQuery = queryFactory
                .select(member.count())
                .from(member)
                .leftJoin(member.team, team)
                .where(memberNameEq(condition.getUserName()),
                        teamNameEq(condition.getTeamName()),
                        memberAgeGoe(condition.getAgeGoe()),
                        memberAgeLoe(condition.getAgeLoe()));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
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
