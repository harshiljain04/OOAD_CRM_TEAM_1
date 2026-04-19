#!/usr/bin/env bash
set -u

pass=0
fail=0

report() {
  local name="$1"
  local ok="$2"
  local details="$3"
  if [[ "$ok" == "PASS" ]]; then
    pass=$((pass+1))
  else
    fail=$((fail+1))
  fi
  printf '%s | %s | %s\n' "$name" "$ok" "$details"
}

call_get() {
  local url="$1"
  curl -s "$url"
}

call_post() {
  local url="$1"
  local data="$2"
  curl -s -X POST "$url" -d "$data"
}

expect_ok_true() {
  local resp="$1"
  [[ "$resp" == *'"ok":true'* ]]
}

expect_ok_false() {
  local resp="$1"
  [[ "$resp" == *'"ok":false'* ]]
}

r=$(call_get 'http://localhost:8080/api/health')
if expect_ok_true "$r"; then report 'health' PASS "$r"; else report 'health' FAIL "$r"; fi

r=$(call_post 'http://localhost:8080/api/leads/create' 'name=LeadOne&email=leadone%40example.com')
lead1=$(echo "$r" | sed -n 's/.*"leadId":\([0-9]*\).*/\1/p')
if expect_ok_true "$r" && [[ -n "$lead1" ]]; then report 'lead.create.valid' PASS "$r"; else report 'lead.create.valid' FAIL "$r"; fi

r=$(call_post 'http://localhost:8080/api/leads/create' 'name=&email=x%40x.com')
if expect_ok_false "$r"; then report 'lead.create.invalid.name' PASS "$r"; else report 'lead.create.invalid.name' FAIL "$r"; fi

r=$(call_post 'http://localhost:8080/api/leads/create' 'name=NoEmail&email=')
if expect_ok_false "$r"; then report 'lead.create.invalid.email' PASS "$r"; else report 'lead.create.invalid.email' FAIL "$r"; fi

r=$(call_get "http://localhost:8080/api/leads/status?id=${lead1}")
if expect_ok_true "$r" && [[ "$r" == *'LEAD'* ]]; then report 'lead.status.valid' PASS "$r"; else report 'lead.status.valid' FAIL "$r"; fi

r=$(call_get 'http://localhost:8080/api/leads/status?id=abc')
if expect_ok_false "$r"; then report 'lead.status.invalid.id' PASS "$r"; else report 'lead.status.invalid.id' FAIL "$r"; fi

r=$(call_post 'http://localhost:8080/api/leads/advance' "id=${lead1}")
if expect_ok_true "$r" && [[ "$r" == *'OPPORTUNITY'* ]]; then report 'lead.advance.valid' PASS "$r"; else report 'lead.advance.valid' FAIL "$r"; fi

r=$(call_post 'http://localhost:8080/api/leads/advance' 'id=zzz')
if expect_ok_false "$r"; then report 'lead.advance.invalid.id' PASS "$r"; else report 'lead.advance.invalid.id' FAIL "$r"; fi

r=$(call_post 'http://localhost:8080/api/customers/create' 'name=CustOne&email=custone%40example.com')
cust1=$(echo "$r" | sed -n 's/.*"customerId":\([0-9]*\).*/\1/p')
if expect_ok_true "$r" && [[ -n "$cust1" ]]; then report 'customer.create.valid' PASS "$r"; else report 'customer.create.valid' FAIL "$r"; fi

r=$(call_post 'http://localhost:8080/api/customers/create' 'name=&email=a%40a.com')
if expect_ok_false "$r"; then report 'customer.create.invalid.name' PASS "$r"; else report 'customer.create.invalid.name' FAIL "$r"; fi

r=$(call_post 'http://localhost:8080/api/customers/create' 'name=A&email=')
if expect_ok_false "$r"; then report 'customer.create.invalid.email' PASS "$r"; else report 'customer.create.invalid.email' FAIL "$r"; fi

r=$(call_post 'http://localhost:8080/api/customers/update' "id=${cust1}&name=CustOneUpdated&email=custone.updated%40example.com")
if expect_ok_true "$r" && [[ "$r" == *'"updated":1'* ]]; then report 'customer.update.valid' PASS "$r"; else report 'customer.update.valid' FAIL "$r"; fi

r=$(call_post 'http://localhost:8080/api/customers/update' 'id=bad&name=X&email=x%40x.com')
if expect_ok_false "$r"; then report 'customer.update.invalid.id' PASS "$r"; else report 'customer.update.invalid.id' FAIL "$r"; fi

r=$(call_get 'http://localhost:8080/api/customers/list')
if expect_ok_true "$r"; then report 'customer.list' PASS "$r"; else report 'customer.list' FAIL "$r"; fi

r=$(call_post 'http://localhost:8080/api/customers/sync' "id=${cust1}")
if expect_ok_true "$r"; then report 'customer.sync.valid' PASS "$r"; else report 'customer.sync.valid' FAIL "$r"; fi

r=$(call_post 'http://localhost:8080/api/customers/sync' 'id=bad')
if expect_ok_false "$r"; then report 'customer.sync.invalid.id' PASS "$r"; else report 'customer.sync.invalid.id' FAIL "$r"; fi

r=$(call_post 'http://localhost:8080/api/interactions/log' "leadId=${lead1}&customerId=${cust1}&type=meeting&notes=Discovery%20Call")
if expect_ok_true "$r"; then report 'interaction.log.valid.all' PASS "$r"; else report 'interaction.log.valid.all' FAIL "$r"; fi

r=$(call_post 'http://localhost:8080/api/interactions/log' "leadId=${lead1}&type=call&notes=LeadOnly")
if expect_ok_true "$r"; then report 'interaction.log.valid.leadOnly' PASS "$r"; else report 'interaction.log.valid.leadOnly' FAIL "$r"; fi

r=$(call_post 'http://localhost:8080/api/interactions/log' "customerId=${cust1}&type=email&notes=CustomerOnly")
if expect_ok_true "$r"; then report 'interaction.log.valid.customerOnly' PASS "$r"; else report 'interaction.log.valid.customerOnly' FAIL "$r"; fi

r=$(call_post 'http://localhost:8080/api/interactions/log' 'type=call&notes=NoRefs')
if expect_ok_false "$r"; then report 'interaction.log.invalid.noRefs' PASS "$r"; else report 'interaction.log.invalid.noRefs' FAIL "$r"; fi

r=$(call_get 'http://localhost:8080/api/interactions/list')
if expect_ok_true "$r" && [[ "$r" == *'Discovery Call'* ]] && [[ "$r" == *'LeadOnly'* ]] && [[ "$r" == *'CustomerOnly'* ]]; then report 'interaction.list.notesRoundTrip' PASS "$r"; else report 'interaction.list.notesRoundTrip' FAIL "$r"; fi

r=$(call_post 'http://localhost:8080/api/campaigns/create' 'name=CampOne&audience=SMB&budget=1500&startDate=2026-04-20&endDate=2026-04-30')
camp1=$(echo "$r" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')
if expect_ok_true "$r" && [[ -n "$camp1" ]]; then report 'campaign.create.valid' PASS "$r"; else report 'campaign.create.valid' FAIL "$r"; fi

r=$(call_post 'http://localhost:8080/api/campaigns/create' 'name=&audience=SMB&budget=1500&startDate=2026-04-20&endDate=2026-04-30')
if expect_ok_false "$r"; then report 'campaign.create.invalid.name' PASS "$r"; else report 'campaign.create.invalid.name' FAIL "$r"; fi

r=$(call_post 'http://localhost:8080/api/campaigns/create' 'name=CampBadAudience&audience=&budget=1500&startDate=2026-04-20&endDate=2026-04-30')
if expect_ok_false "$r"; then report 'campaign.create.invalid.audience' PASS "$r"; else report 'campaign.create.invalid.audience' FAIL "$r"; fi

r=$(call_post 'http://localhost:8080/api/campaigns/create' 'name=CampBadBudget&audience=SMB&budget=0&startDate=2026-04-20&endDate=2026-04-30')
if expect_ok_false "$r"; then report 'campaign.create.invalid.budget' PASS "$r"; else report 'campaign.create.invalid.budget' FAIL "$r"; fi

r=$(call_post 'http://localhost:8080/api/campaigns/create' 'name=CampBadDates&audience=SMB&budget=1500&startDate=2026-05-20&endDate=2026-04-20')
if expect_ok_false "$r"; then report 'campaign.create.invalid.dateOrder' PASS "$r"; else report 'campaign.create.invalid.dateOrder' FAIL "$r"; fi

r=$(call_post 'http://localhost:8080/api/campaigns/revenue' "id=${camp1}&revenue=4500")
if expect_ok_true "$r" && [[ "$r" == *'"revenue":4500.0'* ]]; then report 'campaign.revenue.valid' PASS "$r"; else report 'campaign.revenue.valid' FAIL "$r"; fi

r=$(call_post 'http://localhost:8080/api/campaigns/revenue' 'id=badId&revenue=100')
if expect_ok_false "$r"; then report 'campaign.revenue.invalid.id' PASS "$r"; else report 'campaign.revenue.invalid.id' FAIL "$r"; fi

r=$(call_post 'http://localhost:8080/api/campaigns/revenue' "id=${camp1}&revenue=abc")
if expect_ok_false "$r"; then report 'campaign.revenue.invalid.revenue' PASS "$r"; else report 'campaign.revenue.invalid.revenue' FAIL "$r"; fi

r=$(call_post 'http://localhost:8080/api/campaigns/status' "id=${camp1}&status=ACTIVE")
if expect_ok_true "$r" && [[ "$r" == *'"status":"ACTIVE"'* ]]; then report 'campaign.status.valid' PASS "$r"; else report 'campaign.status.valid' FAIL "$r"; fi

r=$(call_post 'http://localhost:8080/api/campaigns/status' 'id=badId&status=ACTIVE')
if expect_ok_false "$r"; then report 'campaign.status.invalid.id' PASS "$r"; else report 'campaign.status.invalid.id' FAIL "$r"; fi

r=$(call_post 'http://localhost:8080/api/campaigns/status' "id=${camp1}&status=WRONG")
if expect_ok_false "$r" && [[ "$r" == *'Invalid status'* ]]; then report 'campaign.status.invalid.status' PASS "$r"; else report 'campaign.status.invalid.status' FAIL "$r"; fi

r=$(call_get 'http://localhost:8080/api/campaigns/list')
if expect_ok_true "$r"; then report 'campaign.list' PASS "$r"; else report 'campaign.list' FAIL "$r"; fi

r=$(call_get 'http://localhost:8080/api/analytics/summary')
if expect_ok_true "$r"; then report 'analytics.summary' PASS "$r"; else report 'analytics.summary' FAIL "$r"; fi

r=$(call_post 'http://localhost:8080/api/leads/create' 'name=ConvertMe&email=convert%40example.com')
lead_convert=$(echo "$r" | sed -n 's/.*"leadId":\([0-9]*\).*/\1/p')
call_post 'http://localhost:8080/api/leads/advance' "id=${lead_convert}" > /dev/null
call_post 'http://localhost:8080/api/leads/advance' "id=${lead_convert}" > /dev/null
r=$(call_get 'http://localhost:8080/api/customers/list')
if expect_ok_true "$r" && [[ "$r" == *'convert@example.com'* ]]; then report 'lead.to.customer.autoCreate' PASS "$r"; else report 'lead.to.customer.autoCreate' FAIL "$r"; fi

printf 'SUMMARY | PASS=%d | FAIL=%d\n' "$pass" "$fail"
