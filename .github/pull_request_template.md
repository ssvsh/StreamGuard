## Summary

<!-- 1-3 bullets on what changed and why -->

## Checklist

- [ ] Unit tests pass locally (`mvn clean verify`)
- [ ] Integration tests pass (`mvn -Pintegration verify`)
- [ ] Dashboard builds (`cd dashboard && npm run build`)
- [ ] If you added metrics, dashboard JSON updated
- [ ] If you changed a public API, README updated
- [ ] If you touched the LLM path, guardrail tests still cover the behavior

## Risk / Rollback

<!-- How do we roll this back if it breaks production? -->