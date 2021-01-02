package members;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Collection<Member> findByAccountUsername(String username);
}
