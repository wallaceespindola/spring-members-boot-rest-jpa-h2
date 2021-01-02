package members;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.VndErrors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Configuration
@ComponentScan
@EnableAutoConfiguration
public class Application {

    @Bean
    CommandLineRunner init(AccountRepository accountRepository, MemberRepository memberRepository) {
        return (evt) ->
                Arrays.asList("ironman,superman,greenlantern,hulk,elasticman,spiderman".split(","))
                        .forEach(a -> {
                            Account account = accountRepository.save(new Account(a, "password"));
                            memberRepository.save(new Member(account, "http://member.com/1/" + a, "A description"));
                            memberRepository.save(new Member(account, "http://member.com/2/" + a, "A description"));
                        });
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

class MemberResource extends ResourceSupport {

    private final Member member;

    public MemberResource(Member member) {
        String username = member.getAccount().getUsername();
        this.member = member;
        this.add(new Link(member.getUri(), "member-uri"));
        this.add(linkTo(MemberRestController.class, username).withRel("members"));
        this.add(linkTo(methodOn(MemberRestController.class, username).readMember(username, member.getId())).withSelfRel());
    }

    public Member getMember() {
        return member;
    }
}

@RestController
@RequestMapping("/{userId}/members")
class MemberRestController {

    private final MemberRepository memberRepository;

    private final AccountRepository accountRepository;

    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<?> add(@PathVariable String userId, @RequestBody Member input) {

        this.validateUser(userId);

        return accountRepository.findByUsername(userId)
                .map(account -> {
                            Member member = memberRepository.save(new Member(account, input.uri, input.description));

                            HttpHeaders httpHeaders = new HttpHeaders();

                            Link forOneMember = new MemberResource(member).getLink("self");
                            httpHeaders.setLocation(URI.create(forOneMember.getHref()));

                            return new ResponseEntity<>(null, httpHeaders, HttpStatus.CREATED);
                        }
                ).get();
    }

    @RequestMapping(value = "/{memberId}", method = RequestMethod.GET)
    MemberResource readMember(@PathVariable String userId, @PathVariable Long memberId) {
        this.validateUser(userId);
        return new MemberResource(this.memberRepository.findOne(memberId));
    }


    @RequestMapping(method = RequestMethod.GET)
    Resources<MemberResource> readMembers(@PathVariable String userId) {

        this.validateUser(userId);

        List<MemberResource> memberResourceList = memberRepository.findByAccountUsername(userId)
                .stream()
                .map(MemberResource::new)
                .collect(Collectors.toList());
        return new Resources<MemberResource>(memberResourceList);
    }

    @Autowired
    MemberRestController(MemberRepository memberRepository,
                           AccountRepository accountRepository) {
        this.memberRepository = memberRepository;
        this.accountRepository = accountRepository;
    }

    private void validateUser(String userId) {
        this.accountRepository.findByUsername(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}

@ControllerAdvice
class MemberControllerAdvice {

    @ResponseBody
    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    VndErrors userNotFoundExceptionHandler(UserNotFoundException ex) {
        return new VndErrors("error", ex.getMessage());
    }
}


class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String userId) {
        super("could not find user '" + userId + "'.");
    }
}
