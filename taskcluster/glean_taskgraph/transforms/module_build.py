# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.


from taskgraph.transforms.base import TransformSequence

from ..build_config import get_version, get_extensions


transforms = TransformSequence()


@transforms.add
def build_task(config, tasks):
    for task in tasks:
        module_info = task["attributes"]["buildconfig"]
        name = module_info["name"]
        version = get_version()

        for i,item in enumerate(task["run"]["gradlew"]):
            task["run"]["gradlew"][i] = task["run"]["gradlew"][i].format(module_name=name)
        task["description"] = task["description"].format(module_name=name)
        task["worker"]["artifacts"] = artifacts = []

        all_extensions = get_extensions(name)
        for publication_name, extensions in all_extensions.items():
            for extension in extensions:
                artifact_filename = f"{publication_name}-{version}{extension}"
                artifacts.append({
                    "name": f"public/build/{artifact_filename}",
                    "path": f"/builds/worker/checkouts/src/build/maven/org/mozilla/telemetry/{publication_name}/{version}/{artifact_filename}",
                    "type": "file",
                })

        yield task
