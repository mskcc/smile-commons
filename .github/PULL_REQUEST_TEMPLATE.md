# A descriptive title

Briefly describe changes proposed in this pull request:
- a
- b 

---
## Troubleshooting

Fix # (see https://help.github.com/en/articles/closing-issues-using-keywords)

**Expected behavior**

**Actual behavior**

**Logs, error output, or stacktrace**

**Steps to reproduce the behavior**

**Tasks**

Include specific tasks in the order they need to be done in. Include links to specific lines of code where the task should happen at. 

- [ ] task 1
- [ ] task 2
- [ ] task 3

--- 
## Crossing T's and dotting I's

Please follow these checklists to help prevent any unexpected issues from being introduced by the changes in this pull request. If an item does not apply then indicate so by surrounding the line item with `~~` to strikethrough the text. See [basic writing and formatting syntax](https://docs.github.com/en/github/writing-on-github/getting-started-with-writing-and-formatting-on-github/basic-writing-and-formatting-syntax) for more information.

### Mocked request data and data model checklist:

**Data checks:**
Updates were made to the mocked incoming request data and/or mocked published request data:
- [ ] [smile-server test data](https://github.com/mskcc/smile-server/tree/master/service/src/test/resources/data)
- [ ] [smile-commons test data](https://github.com/mskcc/smile-commons/tree/master/src/test/resources/data)
- [ ] [smile-label-generator test data](https://github.com/mskcc/smile-label-generator/tree/master/src/test/resources/data)

**Code checks:**
- [ ] The JSON comparator code been updated to handle new changes.
- [ ] Unit tests were updated in relation to updates to the mocked test data.

If no unit tests were updated or added, then please explain why: [insert details here]

**Describe your testing environment:**

- NATS [local, local docker, dev server, production]
- Neo4j [local, local docker, dev server, production]
- SMILE Server [local, local docker, dev server, production]
- Message publishing simulation [nats cli, docker nats cli, smile publisher tool, other (describe below)]

Other: [insert details on how messages were published or simulated for testing]

---
### Screenshots

---
### General checklist:
- [ ] All requested changes and comments have been resolved.
- [ ] The commit log is comprehensible. It follows [7 rules of great commit messages](http://chris.beams.io/posts/git-commit/). For most PRs a single commit should suffice, in some cases multiple topical commits can be useful. During review it is ok to see tiny commits (e.g. Fix reviewer comments), but right before the code gets merged to master or rc branch, any such commits should be squashed since they are useless to the other developers. Definitely avoid [merge commits, use rebase instead.](http://nathanleclaire.com/blog/2014/09/14/dont-be-scared-of-git-rebase/)
