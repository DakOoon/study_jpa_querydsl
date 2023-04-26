package study.querydsl.repository;

import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import java.util.List;

@SpringBootTest
@Transactional
class MemberRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberRepository memberRepository;

    @Test
    public void basicTest() {
        Member member = new Member("member1", 10);
        memberRepository.save(member);

        Member findMember = memberRepository.findById(member.getId()).get();
        Assertions.assertThat(findMember).isEqualTo(member);

        List<Member> results1 = memberRepository.findAll();
        Assertions.assertThat(results1).containsExactly(member);

        List<Member> results2 = memberRepository.findByName("member1");
        Assertions.assertThat(results2).containsExactly(member);
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

        List<MemberTeamDto> results1 = memberRepository.search(condition1);

        // then
        Assertions.assertThat(results1).extracting("userName")
                .containsExactly("member4");

        // when
        MemberSearchCondition condition2 = new MemberSearchCondition();
        condition2.setTeamName("teamB");

        List<MemberTeamDto> results2 = memberRepository.search(condition2);

        // then
        Assertions.assertThat(results2).extracting("userName")
                .containsExactly("member3", "member4");
    }

    @Test
    public void searchPageSimpleTest() {
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

        PageRequest pageRequest1 = PageRequest.of(0, 1, Sort.unsorted());

        Page<MemberTeamDto> results1 = memberRepository.searchPage(condition1, pageRequest1);

        // then
        Assertions.assertThat(results1.getTotalElements()).isEqualTo(1);
        Assertions.assertThat(results1.getTotalPages()).isEqualTo(1);
        Assertions.assertThat(results1.getNumber()).isEqualTo(0);
        Assertions.assertThat(results1.getNumberOfElements()).isEqualTo(1);
        Assertions.assertThat(results1.getContent()).extracting("userName")
                .containsExactly("member4");

        // when
        MemberSearchCondition condition2 = new MemberSearchCondition();
        condition2.setTeamName("teamB");

        PageRequest pageRequest2 = PageRequest.of(1, 1, Sort.unsorted());

        Page<MemberTeamDto> results2 = memberRepository.searchPage(condition2, pageRequest2);

        // then
        Assertions.assertThat(results2.getTotalElements()).isEqualTo(2);
        Assertions.assertThat(results2.getTotalPages()).isEqualTo(2);
        Assertions.assertThat(results2.getNumber()).isEqualTo(1);
        Assertions.assertThat(results2.getNumberOfElements()).isEqualTo(1);
        Assertions.assertThat(results2.getContent()).extracting("userName")
                .containsExactly("member4");
    }
}