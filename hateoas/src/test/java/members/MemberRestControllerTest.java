package members;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * @author Josh Long
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
public class MemberRestControllerTest {


    private MediaType contentType = new MediaType(
            "application", "hal+json");

    private MockMvc mockMvc;

    private String userName = "wallace";

    private HttpMessageConverter mappingJackson2HttpMessageConverter;

    private Account account;

    private List<Member> memberList = new ArrayList<>();

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    void setConverters(HttpMessageConverter<?>[] converters) {

        this.mappingJackson2HttpMessageConverter = Arrays.asList(converters).stream().filter(
                hmc -> hmc instanceof MappingJackson2HttpMessageConverter).findAny().get();

        Assert.assertNotNull("the JSON message converter must not be null",
                this.mappingJackson2HttpMessageConverter);
    }

    @Before
    public void setup() throws Exception {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();

        this.memberRepository.deleteAllInBatch();
        this.accountRepository.deleteAllInBatch();

        this.account = accountRepository.save(new Account(userName, "password"));
        this.memberList.add(memberRepository.save(new Member(account, "http://member.com/1/" + userName, "A description")));
        this.memberList.add(memberRepository.save(new Member(account, "http://member.com/2/" + userName, "A description")));
    }

    @Test
    public void userNotFound() throws Exception {
        mockMvc.perform(post("/george/members/")
                .content(this.json(new Member()))
                .contentType(contentType))
                .andExpect(status().isNotFound());
    }

    @Test
    public void readSingleMember() throws Exception {
        mockMvc.perform(get("/" + userName + "/members/"
                + this.memberList.get(0).getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.member.id", is(this.memberList.get(0).getId().intValue())))
                .andExpect(jsonPath("$.member.uri", is("http://member.com/1/" + userName)))
                .andExpect(jsonPath("$.member.description", is("A description")))
                .andExpect(jsonPath("$._links.self.href", containsString("/" + userName + "/members/"
                        + this.memberList.get(0).getId())));
    }

    @Test
    public void readMembers() throws Exception {
        mockMvc.perform(get("/" + userName + "/members"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.memberResourceList", hasSize(2)))
                .andExpect(jsonPath("$._embedded.memberResourceList[0].member.id", is(this.memberList.get(0).getId().intValue())))
                .andExpect(jsonPath("$._embedded.memberResourceList[0].member.uri", is("http://member.com/1/" + userName)))
                .andExpect(jsonPath("$._embedded.memberResourceList[0].member.description", is("A description")))
                .andExpect(jsonPath("$._embedded.memberResourceList[1].member.id", is(this.memberList.get(1).getId().intValue())))
                .andExpect(jsonPath("$._embedded.memberResourceList[1].member.uri", is("http://member.com/2/" + userName)))
                .andExpect(jsonPath("$._embedded.memberResourceList[1].member.description", is("A description")));
    }

    @Test
    public void createMember() throws Exception {
        String memberJson = json(new Member(
                this.account, "http://spring.io", "a member to the best resource for Spring news and information"));
        this.mockMvc.perform(post("/" + userName + "/members")
                .contentType(contentType)
                .content(memberJson))
                .andExpect(status().isCreated());
    }

    protected String json(Object o) throws IOException {
        MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
        this.mappingJackson2HttpMessageConverter.write(
                o, MediaType.APPLICATION_JSON, mockHttpOutputMessage);
        return mockHttpOutputMessage.getBodyAsString();
    }
}
