// tag::runner[]
package members;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Configuration
@ComponentScan
@EnableAutoConfiguration
public class Application {

	@Bean
	CommandLineRunner init(AccountRepository accountRepository,
			MemberRepository memberRepository) {
		return (evt) -> Arrays.asList(
				"jhoeller,dsyer,pwebb,ogierke,rwinch,mfisher,mpollack,jlong".split(","))
				.forEach(
						a -> {
							Account account = accountRepository.save(new Account(a,
									"password"));
							memberRepository.save(new Member(account,
									"http://member.com/1/" + a, "A description"));
							memberRepository.save(new Member(account,
									"http://member.com/2/" + a, "A description"));
						});
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
// end::runner[]

@RestController
@RequestMapping("/{userId}/members")
class MemberRestController {

	private final MemberRepository memberRepository;

	private final AccountRepository accountRepository;

	@RequestMapping(method = RequestMethod.POST)
	ResponseEntity<?> add(@PathVariable String userId, @RequestBody Member input) {
		this.validateUser(userId);
		return this.accountRepository
				.findByUsername(userId)
				.map(account -> {
					Member result = memberRepository.save(new Member(account,
							input.uri, input.description));

					HttpHeaders httpHeaders = new HttpHeaders();
					httpHeaders.setLocation(ServletUriComponentsBuilder
							.fromCurrentRequest().path("/{id}")
							.buildAndExpand(result.getId()).toUri());
					return new ResponseEntity<>(null, httpHeaders, HttpStatus.CREATED);
				}).get();

	}

	@RequestMapping(value = "/{memberId}", method = RequestMethod.GET)
	Member readMember(@PathVariable String userId, @PathVariable Long memberId) {
		this.validateUser(userId);
		return this.memberRepository.findOne(memberId);
	}

	@RequestMapping(method = RequestMethod.GET)
	Collection<Member> readMembers(@PathVariable String userId) {
		this.validateUser(userId);
		return this.memberRepository.findByAccountUsername(userId);
	}

	@Autowired
	MemberRestController(MemberRepository memberRepository,
			AccountRepository accountRepository) {
		this.memberRepository = memberRepository;
		this.accountRepository = accountRepository;
	}

	private void validateUser(String userId) {
		this.accountRepository.findByUsername(userId).orElseThrow(
				() -> new UserNotFoundException(userId));
	}
}

@ResponseStatus(HttpStatus.NOT_FOUND)
class UserNotFoundException extends RuntimeException {

	public UserNotFoundException(String userId) {
		super("could not find user '" + userId + "'.");
	}
}
