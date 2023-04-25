package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import java.util.List;

import static study.querydsl.entity.QMember.*;

@SpringBootTest
@Transactional
public class QuerydslIntermediateTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void findDtoByJPQL() {
        List<MemberDto> results = em.createQuery("select new study.querydsl.dto.MemberDto(m.name, m.age) " +
                        "from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto result : results) {
            System.out.println("memberDto = " + result);
        }
    }

    @Test
    public void findDtoBySetter() {
        List<MemberDto> results = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.name,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto result : results) {
            System.out.println("memberDto = " + result);
        }
    }

    @Test
    public void findDtoByField() {
        List<MemberDto> results = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.name,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto result : results) {
            System.out.println("memberDto = " + result);
        }
    }

    @Test
    public void findDtoByConstructor() {
        List<MemberDto> results = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.name,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto result : results) {
            System.out.println("memberDto = " + result);
        }
    }

    @Test
    public void findUserDto() {
        QMember memberSub = new QMember("memberSub");

        List<UserDto> results = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.name.as("username"),
                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age")))
                .from(member)
                .fetch();

        for (UserDto result : results) {
            System.out.println("UserDto = " + result);
        }
    }

    @Test
    public void findDtyByQueryProjection() {
        List<MemberDto> results = queryFactory
                .select(new QMemberDto(
                        member.name,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto result : results) {
            System.out.println("memberDto = " + result);
        }
    }

    @Test
    public void dynamicQuery_BooleanBuilder() {
        String nameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(nameParam, ageParam);
        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String nameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder();
        if (nameCond != null) {
            builder.and(member.name.eq(nameCond));
        }
        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    @Test
    public void dynamicQuery_WhereParam() {
        String nameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(nameParam, ageParam);
        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String nameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(nameEq(nameCond), ageEq(ageCond))
                .fetch();
    }

    private BooleanExpression nameEq(String nameCond) {
        return nameCond != null ? member.name.eq(nameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    private BooleanExpression allEq(String nameCond, Integer ageCond) {
        return nameEq(nameCond).and(ageEq(ageCond));
    }

    @Test
    public void bulkUpdate() {
        long count = queryFactory
                .update(member)
                .set(member.name, "비회원")
                .where(member.age.lt(28))
                .execute();

        em.flush();
        em.clear();

        List<Member> results = queryFactory
                .selectFrom(member)
                .fetch();
        for (Member result : results) {
            System.out.println("result = " + result);
        }
        Assertions.assertThat(results.get(0).getName()).isEqualTo("비회원");
        Assertions.assertThat(results.get(1).getName()).isEqualTo("비회원");
        Assertions.assertThat(results.get(2).getName()).isEqualTo("member3");
        Assertions.assertThat(results.get(3).getName()).isEqualTo("member4");
    }

    @Test
    public void bulkAdd() {
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(-1))
                .execute();

        em.flush();
        em.clear();

        List<Member> results = queryFactory
                .selectFrom(member)
                .fetch();
        for (Member result : results) {
            System.out.println("result = " + result);
        }
        Assertions.assertThat(results.get(0).getAge()).isEqualTo(9);
        Assertions.assertThat(results.get(1).getAge()).isEqualTo(19);
        Assertions.assertThat(results.get(2).getAge()).isEqualTo(29);
        Assertions.assertThat(results.get(3).getAge()).isEqualTo(39);
    }
    
    @Test
    public void bulkDelete() {
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();

        List<Member> results = queryFactory
                .selectFrom(member)
                .fetch();
        for (Member result : results) {
            System.out.println("result = " + result);
        }
        Assertions.assertThat(results.size()).isEqualTo(1);
    }

    @Test
    public void sqlFunction1() {
        List<String> results = queryFactory
                .select(Expressions.stringTemplate(
                        "function('replace', {0}, {1}, {2})",
                        member.name, "member", "M"))
                .from(member)
                .fetch();

        Assertions.assertThat(results.get(0)).isEqualTo("M1");
        Assertions.assertThat(results.get(1)).isEqualTo("M2");
        Assertions.assertThat(results.get(2)).isEqualTo("M3");
        Assertions.assertThat(results.get(3)).isEqualTo("M4");
    }

    @Test
    public void sqlFunction2() {
        List<String> results = queryFactory
                .select(member.name)
                .from(member)
//                .where(member.name.eq(Expressions.stringTemplate(
//                        "function('lower', {0})",
//                        member.name)))
                .where(member.name.eq(member.name.lower()))
                .fetch();
    }
}
