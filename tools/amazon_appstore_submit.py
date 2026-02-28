import json
import sys
from pathlib import Path

import requests

# Args: client_id, client_secret, app_id, apk_path
client_id = sys.argv[1]
client_secret = sys.argv[2]
app_id = sys.argv[3]
apk_path = sys.argv[4]

BASE_URL = "https://developer.amazon.com/api/appstore"

scope = "appstore::apps:readwrite"
grant_type = "client_credentials"

print("-- Amazon Appstore: requesting access token --")
resp = requests.post(
    "https://api.amazon.com/auth/o2/token",
    data={
        "grant_type": grant_type,
        "client_id": client_id,
        "client_secret": client_secret,
        "scope": scope,
    },
)
resp.raise_for_status()
access_token = resp.json()["access_token"]
headers = {"Authorization": f"Bearer {access_token}"}

print("-- Amazon Appstore: creating or fetching edit --")
create_edit_url = f"{BASE_URL}/v1/applications/{app_id}/edits"
create_edit = requests.post(create_edit_url, headers=headers)
if create_edit.status_code in (200, 201):
    edit_id = create_edit.json()["id"]
    edit_etag = create_edit.headers.get("ETag", "")
else:
    get_edit = requests.get(create_edit_url, headers=headers)
    get_edit.raise_for_status()
    edit_id = get_edit.json()["id"]
    edit_etag = get_edit.headers.get("ETag", "")

print(f"-- Amazon Appstore: edit id {edit_id} --")

print("-- Amazon Appstore: listing current APKs --")
apks_url = f"{BASE_URL}/v1/applications/{app_id}/edits/{edit_id}/apks"
apks_resp = requests.get(apks_url, headers=headers)
apks_resp.raise_for_status()
apks = apks_resp.json()
if not apks:
    raise RuntimeError("No APKs found in existing edit. First submission must be done in console.")

apk_id = apks[0]["id"]
print(f"-- Amazon Appstore: replacing APK {apk_id} --")
apk_info_url = f"{BASE_URL}/v1/applications/{app_id}/edits/{edit_id}/apks/{apk_id}"
apk_info = requests.get(apk_info_url, headers=headers)
apk_info.raise_for_status()
apk_etag = apk_info.headers.get("ETag", "")

apk_file = Path(apk_path)
if not apk_file.exists():
    raise FileNotFoundError(apk_path)

with apk_file.open("rb") as f:
    replace_url = f"{BASE_URL}/v1/applications/{app_id}/edits/{edit_id}/apks/{apk_id}/replace"
    replace_headers = {
        "Content-Type": "application/vnd.android.package-archive",
        "If-Match": apk_etag,
        "fileName": apk_file.stem,
    }
    replace_headers.update(headers)
    replace_resp = requests.put(replace_url, headers=replace_headers, data=f.read())
    replace_resp.raise_for_status()

print("-- Amazon Appstore: validating edit --")
validate_url = f"{BASE_URL}/v1/applications/{app_id}/edits/{edit_id}/validate"
validate_resp = requests.post(validate_url, headers=headers)
validate_resp.raise_for_status()

print("-- Amazon Appstore: validation response --")
print(json.dumps(validate_resp.json(), indent=2))
