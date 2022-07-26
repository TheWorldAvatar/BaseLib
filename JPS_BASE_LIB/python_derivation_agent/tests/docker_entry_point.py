import tests.conftest as cft

def create_rng_app():
    rng_agent = cft.create_rng_agent()

    rng_agent.add_job_monitoring_derivations(start=True)

    return rng_agent.app


def create_max_app():
    max_agent = cft.create_max_agent()

    max_agent.add_job_monitoring_derivations(start=True)

    return max_agent.app


def create_min_app():
    min_agent = cft.create_min_agent()

    min_agent.add_job_monitoring_derivations(start=True)

    return min_agent.app


def create_diff_app():
    diff_agent = cft.create_diff_agent()

    diff_agent.add_job_monitoring_derivations(start=True)

    return diff_agent.app


def create_update_endpoint():
    update_agent = cft.create_update_endpoint()

    update_agent.add_url_pattern(
        '/update', 'update_derivation',
        update_agent.update_derivations, methods=['GET']
    )

    return update_agent.app
