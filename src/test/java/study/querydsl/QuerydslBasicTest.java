package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import java.util.List;

import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

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
    public void startJPQL() {
        // find member1
        String qlString =
                "select m from Member m " +
                "where m.name = :name";

        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("name", "member1")
                .getSingleResult();

        Assertions.assertThat(findMember.getName()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl() {
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.name.eq("member1"))
                .fetchOne();

        Assertions.assertThat(findMember.getName()).isEqualTo("member1");
    }

    @Test
    public void search() {
        Member findMember1 = queryFactory
                .selectFrom(member)
                .where(member.name.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        Member findMember2 = queryFactory
                .selectFrom(member)
                .where(
                        member.name.eq("member1"),
                        member.age.eq(10))
                .fetchOne();

        Assertions.assertThat(findMember2.getName()).isEqualTo("member1");
    }

    @Test
    public void resultFetch() {
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        Member fetchOne = queryFactory
                .selectFrom(member)
                .limit(1)
                .fetchOne();

        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst();

        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();
        long total1 = results.getTotal();
        List<Member> contents = results.getResults();

        long total2 = queryFactory
                .selectFrom(member)
                .fetchCount();
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 오름차순(asc)
     * 단, 2 에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> results = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.name.asc().nullsLast())
                .fetch();

        Member member5 = results.get(0);
        Member member6 = results.get(1);
        Member memberNull = results.get(2);
        Assertions.assertThat(member5.getName()).isEqualTo("member5");
        Assertions.assertThat(member6.getName()).isEqualTo("member6");
        Assertions.assertThat(memberNull.getName()).isNull();
    }

    @Test
    public void paging1() {
        List<Member> results = queryFactory
                .selectFrom(member)
                .orderBy(member.name.desc())
                .offset(1)
                .limit(2)
                .fetch();

        Member member3 = results.get(0);
        Member member2 = results.get(1);
        Assertions.assertThat(results.size()).isEqualTo(2);
        Assertions.assertThat(member3.getName()).isEqualTo("member3");
        Assertions.assertThat(member2.getName()).isEqualTo("member2");
    }

    @Test
    public void paging2() {
        List<Member> results = queryFactory
                .selectFrom(member)
                .orderBy(member.name.desc())
                .offset(1)
                .limit(2)
                .fetch();

        /* deprecated
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.name.desc())
                .offset(1)
                .limit(2)
                .fetchResults();
        */
        Long total = queryFactory
                .select(member.count())
                .from(member)
                .fetchOne();

        Member member3 = results.get(0);
        Member member2 = results.get(1);
        Assertions.assertThat(results.size()).isEqualTo(2);
        Assertions.assertThat(member3.getName()).isEqualTo("member3");
        Assertions.assertThat(member2.getName()).isEqualTo("member2");
        Assertions.assertThat(total).isEqualTo(4);
    }

    @Test
    public void aggregation() {
        List<Tuple> results = queryFactory
                .select(member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min())
                .from(member)
                .fetch();

        Tuple tuple = results.get(0);
        Assertions.assertThat(tuple.get(member.count())).isEqualTo(4);
        Assertions.assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        Assertions.assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        Assertions.assertThat(tuple.get(member.age.max())).isEqualTo(40);
        Assertions.assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구하라.
     */
    @Test
    public void group() {
        List<Tuple> results = queryFactory
                .select(team.name,
                        member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = results.get(0);
        Tuple teamB = results.get(1);

        Assertions.assertThat(teamA.get(team.name)).isEqualTo("teamA");
        Assertions.assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        Assertions.assertThat(teamB.get(team.name)).isEqualTo("teamB");
        Assertions.assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /**
     * 팀 A에 소속된 모든 회원
     */
    @Test
    public void join() {
        List<Member> results = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        Assertions.assertThat(results)
                .extracting("name")
                .containsExactly("member1", "member2");
    }

    /**
     * 세타 조인
     * 회원의 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    public void theta_join() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> results = queryFactory
                .select(member)
                .from(member, team)
                .where(member.name.eq(team.name))
                .fetch();

        Assertions.assertThat(results)
                .extracting("name")
                .containsExactly("teamA", "teamB");
    }

    /**
     * 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL: select m, t from Member m left join m.team t on t.name = 'teamA'
     */
    @Test
    public void join_on_filtering() {
        List<Tuple> results = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .orderBy(member.name.asc())
                .fetch();

        Assertions.assertThat(results.size()).isEqualTo(4);
        Assertions.assertThat(results.get(0).get(team).getName()).isEqualTo("teamA");
        Assertions.assertThat(results.get(1).get(team).getName()).isEqualTo("teamA");
        Assertions.assertThat(results.get(2).get(team)).isNull();
        Assertions.assertThat(results.get(3).get(team)).isNull();
    }

    /**
     * 연관관계가 없는 엔티티 외부 조인
     * 회원의 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    public void join_on_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Tuple> results = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.name.eq(team.name))
                .orderBy(member.name.asc())
                .fetch();
        for (Tuple result : results) {
            System.out.println("result = " + result);
        }
        Assertions.assertThat(results.size()).isEqualTo(6);
        Assertions.assertThat(results.get(0).get(team)).isNull();
        Assertions.assertThat(results.get(1).get(team)).isNull();
        Assertions.assertThat(results.get(2).get(team)).isNull();
        Assertions.assertThat(results.get(3).get(team)).isNull();
        Assertions.assertThat(results.get(4).get(team).getName()).isEqualTo("teamA");
        Assertions.assertThat(results.get(5).get(team).getName()).isEqualTo("teamB");
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.name.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        Assertions.assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    @Test
    public void fetchJoinUse() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.name.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        Assertions.assertThat(loaded).as("페치 조인 적용").isTrue();
    }

    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    public void subQuery() {

        QMember memberSub = new QMember("memberSub");

        List<Member> results = queryFactory
                .selectFrom(member)
                .where(member.age.eq(JPAExpressions
                        .select(memberSub.age.max())
                        .from(memberSub)))
                .fetch();

        Assertions.assertThat(results.size()).isEqualTo(1);
        Assertions.assertThat(results.get(0).getName()).isEqualTo("member4");
    }

    /**
     * 나이가 평균 이상인 회원 조회
     */
    @Test
    public void subQueryGoe() {

        QMember memberSub = new QMember("memberSub");

        List<Member> results = queryFactory
                .selectFrom(member)
                .where(member.age.goe(JPAExpressions
                        .select(memberSub.age.avg())
                        .from(memberSub)))
                .fetch();

        Assertions.assertThat(results.size()).isEqualTo(2);
        Assertions.assertThat(results).extracting("age")
                .containsExactly(30, 40);
    }

    @Test
    public void subQueryIn() {

        QMember memberSub = new QMember("memberSub");

        List<Member> results = queryFactory
                .selectFrom(member)
                .where(member.age.in(JPAExpressions
                        .select(memberSub.age)
                        .from(memberSub)
                        .where(member.age.gt(10))))
                .fetch();

        Assertions.assertThat(results.size()).isEqualTo(3);
        Assertions.assertThat(results).extracting("age")
                .containsExactly(20, 30, 40);
    }

    @Test
    public void subQuerySelect() {

        QMember memberSub = new QMember("memberSub");

        List<Tuple> results = queryFactory
                .select(member.name, ExpressionUtils.as(JPAExpressions
                        .select(memberSub.age.avg())
                        .from(memberSub), "avg"))
                .from(member)
                .fetch();

        Assertions.assertThat(results.get(0).get(member.name)).isEqualTo("member1");
        Assertions.assertThat(results.get(0).get(Expressions.numberPath(Double.class, "avg")))
                .isEqualTo(25);
    }

    @Test
    public void caseBasic() {
        List<String> results = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        Assertions.assertThat(results.get(0)).isEqualTo("열살");
        Assertions.assertThat(results.get(1)).isEqualTo("스무살");
        Assertions.assertThat(results.get(2)).isEqualTo("기타");
        Assertions.assertThat(results.get(3)).isEqualTo("기타");
    }

    @Test
    public void caseComplex() {
        List<String> results = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        Assertions.assertThat(results.get(0)).isEqualTo("0~20살");
        Assertions.assertThat(results.get(1)).isEqualTo("0~20살");
        Assertions.assertThat(results.get(2)).isEqualTo("21~30살");
        Assertions.assertThat(results.get(3)).isEqualTo("기타");
    }

    @Test
    public void constant() {
        List<Tuple> results = queryFactory
                .select(member.name,
                        Expressions.constant("A"))
                .from(member)
                .fetch();

        Assertions.assertThat(results.size()).isEqualTo(4);
        Assertions.assertThat(results.get(0).get(0, String.class)).isEqualTo("member1");
        Assertions.assertThat(results.get(0).get(1, String.class)).isEqualTo("A");
    }

    @Test
    public void concat() {
        // {name}_{age}
        List<String> results = queryFactory
                .select(member.name.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.name.eq("member1"))
                .fetch();

        Assertions.assertThat(results.size()).isEqualTo(1);
        Assertions.assertThat(results.get(0)).isEqualTo("member1_10");
    }
}
