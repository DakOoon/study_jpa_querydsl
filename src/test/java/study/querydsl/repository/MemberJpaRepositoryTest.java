package study.querydsl.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import java.util.List;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberJpaRepository memberJpaRepository;

    @Test
    public void basicTest() {
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        Member findMember = memberJpaRepository.findById(member.getId()).get();
        Assertions.assertThat(findMember).isEqualTo(member);

        List<Member> results1 = memberJpaRepository.findAll();
        Assertions.assertThat(results1).containsExactly(member);

        List<Member> results2 = memberJpaRepository.findByName("member1");
        Assertions.assertThat(results2).containsExactly(member);

        List<Member> results3 = memberJpaRepository.findAll_Querydsl();
        Assertions.assertThat(results3).containsExactly(member);

        List<Member> results4 = memberJpaRepository.findByName_Querydsl("member1");
        Assertions.assertThat(results4).containsExactly(member);
    }

    @Test
    public void searchByBuilderTest() {
        // given
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

        // when
        MemberSearchCondition condition1 = new MemberSearchCondition();
        condition1.setAgeGoe(35);
        condition1.setAgeLoe(40);
        condition1.setTeamName("teamB");

        List<MemberTeamDto> results1 = memberJpaRepository.searchByBuilder(condition1);

        // then
        Assertions.assertThat(results1).extracting("userName")
                .containsExactly("member4");

        // when
        MemberSearchCondition condition2 = new MemberSearchCondition();
        condition2.setTeamName("teamB");

        List<MemberTeamDto> results2 = memberJpaRepository.searchByBuilder(condition2);

        // then
        Assertions.assertThat(results2).extracting("userName")
                .containsExactly("member3", "member4");
    }

    @Test
    public void searchTest() {
        // given
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

        // when
        MemberSearchCondition condition1 = new MemberSearchCondition();
        condition1.setAgeGoe(35);
        condition1.setAgeLoe(40);
        condition1.setTeamName("teamB");

        List<MemberTeamDto> results1 = memberJpaRepository.search(condition1);

        // then
        Assertions.assertThat(results1).extracting("userName")
                .containsExactly("member4");

        // when
        MemberSearchCondition condition2 = new MemberSearchCondition();
        condition2.setTeamName("teamB");

        List<MemberTeamDto> results2 = memberJpaRepository.search(condition2);

        // then
        Assertions.assertThat(results2).extracting("userName")
                .containsExactly("member3", "member4");
    }
}